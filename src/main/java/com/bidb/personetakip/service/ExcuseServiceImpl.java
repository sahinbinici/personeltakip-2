package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.ExcuseRequestDto;
import com.bidb.personetakip.dto.ExcuseResponseDto;
import com.bidb.personetakip.dto.ExcuseTypeDto;
import com.bidb.personetakip.model.Excuse;
import com.bidb.personetakip.model.ExcuseStatus;
import com.bidb.personetakip.repository.ExcuseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ExcuseService for managing excuse operations.
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 8.3, 8.4, 8.5
 */
@Service
@Transactional
public class ExcuseServiceImpl implements ExcuseService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcuseServiceImpl.class);
    
    private final ExcuseRepository excuseRepository;
    private final ObjectMapper objectMapper;
    
    // Predefined excuse types (in a real application, these might come from a database)
    private static final List<ExcuseTypeDto> EXCUSE_TYPES = Arrays.asList(
        new ExcuseTypeDto(1L, "Hastalık", true, false),
        new ExcuseTypeDto(2L, "Aile Acil Durumu", true, false),
        new ExcuseTypeDto(3L, "Resmi İzin", false, true),
        new ExcuseTypeDto(4L, "Ulaşım Sorunu", true, false),
        new ExcuseTypeDto(5L, "Diğer", true, false)
    );
    
    public ExcuseServiceImpl(ExcuseRepository excuseRepository, ObjectMapper objectMapper) {
        this.excuseRepository = excuseRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ExcuseResponseDto submitExcuse(Long userId, ExcuseRequestDto excuseRequest) {
        logger.info("Submitting excuse for user {} for date {}", userId, excuseRequest.date());
        
        try {
            // Validate the request
            String validationError = validateExcuseRequest(excuseRequest);
            if (validationError != null) {
                logger.warn("Excuse validation failed for user {}: {}", userId, validationError);
                return ExcuseResponseDto.error(validationError);
            }
            
            // Check if user already has an excuse for this date
            if (hasExcuseForDate(userId, excuseRequest.date())) {
                String error = "Bu tarih için zaten bir mazeret bildirimi bulunmaktadır.";
                logger.warn("Duplicate excuse attempt for user {} on date {}", userId, excuseRequest.date());
                return ExcuseResponseDto.error(error);
            }
            
            // Create and save excuse
            Excuse excuse = new Excuse(
                userId,
                excuseRequest.excuseType().id(),
                excuseRequest.excuseType().name(),
                excuseRequest.description(),
                excuseRequest.date()
            );
            
            // Handle attachments if present
            if (excuseRequest.attachments() != null && !excuseRequest.attachments().isEmpty()) {
                try {
                    String attachmentsJson = objectMapper.writeValueAsString(excuseRequest.attachments());
                    excuse.setAttachments(attachmentsJson);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to serialize attachments for user {}", userId, e);
                    return ExcuseResponseDto.error("Ek dosyalar işlenirken hata oluştu.");
                }
            }
            
            excuse = excuseRepository.save(excuse);
            
            logger.info("Excuse submitted successfully with ID {} for user {}", excuse.getId(), userId);
            return ExcuseResponseDto.success(excuse.getId(), userId);
            
        } catch (Exception e) {
            logger.error("Failed to submit excuse for user {}", userId, e);
            return ExcuseResponseDto.error("Mazeret bildirimi gönderilirken sistem hatası oluştu.");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExcuseTypeDto> getExcuseTypes() {
        logger.debug("Retrieving excuse types");
        return new ArrayList<>(EXCUSE_TYPES);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Excuse> getUserExcuses(Long userId, Pageable pageable) {
        logger.debug("Retrieving excuses for user {} with pagination", userId);
        return excuseRepository.findByUserIdOrderBySubmittedAtDesc(userId, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Excuse> getUserExcusesByStatus(Long userId, ExcuseStatus status, Pageable pageable) {
        logger.debug("Retrieving excuses for user {} with status {} and pagination", userId, status);
        return excuseRepository.findByUserIdAndStatusOrderBySubmittedAtDesc(userId, status, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Excuse> getUserExcusesByDateRange(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        logger.debug("Retrieving excuses for user {} between {} and {}", userId, startDate, endDate);
        return excuseRepository.findByUserIdAndExcuseDateBetweenOrderByExcuseDateDesc(userId, startDate, endDate, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Excuse> getPendingExcusesForAdmin(Pageable pageable) {
        logger.debug("Retrieving pending excuses for admin review");
        return excuseRepository.findPendingExcusesForAdmin(pageable);
    }
    
    @Override
    public Excuse approveExcuse(Long excuseId, Long adminId, String notes) {
        logger.info("Approving excuse {} by admin {}", excuseId, adminId);
        
        Optional<Excuse> excuseOpt = excuseRepository.findById(excuseId);
        if (excuseOpt.isEmpty()) {
            throw new IllegalArgumentException("Excuse not found with ID: " + excuseId);
        }
        
        Excuse excuse = excuseOpt.get();
        if (!excuse.isPending()) {
            throw new IllegalStateException("Excuse is not in pending status");
        }
        
        excuse.approve(adminId, notes);
        excuse = excuseRepository.save(excuse);
        
        logger.info("Excuse {} approved successfully by admin {}", excuseId, adminId);
        return excuse;
    }
    
    @Override
    public Excuse rejectExcuse(Long excuseId, Long adminId, String notes) {
        logger.info("Rejecting excuse {} by admin {}", excuseId, adminId);
        
        Optional<Excuse> excuseOpt = excuseRepository.findById(excuseId);
        if (excuseOpt.isEmpty()) {
            throw new IllegalArgumentException("Excuse not found with ID: " + excuseId);
        }
        
        Excuse excuse = excuseOpt.get();
        if (!excuse.isPending()) {
            throw new IllegalStateException("Excuse is not in pending status");
        }
        
        excuse.reject(adminId, notes);
        excuse = excuseRepository.save(excuse);
        
        logger.info("Excuse {} rejected successfully by admin {}", excuseId, adminId);
        return excuse;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Excuse> getExcuseById(Long excuseId) {
        logger.debug("Retrieving excuse by ID: {}", excuseId);
        return excuseRepository.findById(excuseId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasExcuseForDate(Long userId, LocalDate date) {
        logger.debug("Checking if user {} has excuse for date {}", userId, date);
        return excuseRepository.findByUserIdAndExcuseDate(userId, date).isPresent();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getPendingExcuseCount(Long userId) {
        logger.debug("Getting pending excuse count for user {}", userId);
        return excuseRepository.countByUserIdAndStatus(userId, ExcuseStatus.PENDING);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalPendingExcuseCount() {
        logger.debug("Getting total pending excuse count");
        return excuseRepository.countByStatus(ExcuseStatus.PENDING);
    }
    
    @Override
    public String validateExcuseRequest(ExcuseRequestDto excuseRequest) {
        if (excuseRequest == null) {
            return "Mazeret bilgisi gerekli";
        }
        
        // Use the validation method from the DTO
        return excuseRequest.getValidationError();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<ExcuseStatus, Long> getExcuseStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting excuse statistics for user {} between {} and {}", userId, startDate, endDate);
        
        List<Object[]> results = excuseRepository.getExcuseStatistics(userId, startDate, endDate);
        
        Map<ExcuseStatus, Long> statistics = new EnumMap<>(ExcuseStatus.class);
        
        // Initialize all statuses with 0
        for (ExcuseStatus status : ExcuseStatus.values()) {
            statistics.put(status, 0L);
        }
        
        // Fill in actual counts
        for (Object[] result : results) {
            ExcuseStatus status = (ExcuseStatus) result[0];
            Long count = (Long) result[1];
            statistics.put(status, count);
        }
        
        return statistics;
    }
    
    /**
     * Gets excuse type by ID.
     * 
     * @param excuseTypeId Excuse type ID
     * @return Optional excuse type
     */
    private Optional<ExcuseTypeDto> getExcuseTypeById(Long excuseTypeId) {
        return EXCUSE_TYPES.stream()
            .filter(type -> type.id().equals(excuseTypeId))
            .findFirst();
    }
    
    /**
     * Parses attachments JSON string to list.
     * 
     * @param attachmentsJson JSON string
     * @return List of attachment paths
     */
    private List<String> parseAttachments(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(attachmentsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse attachments JSON: {}", attachmentsJson, e);
            return Collections.emptyList();
        }
    }
}