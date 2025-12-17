package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.ExcuseRequestDto;
import com.bidb.personetakip.dto.ExcuseResponseDto;
import com.bidb.personetakip.dto.ExcuseTypeDto;
import com.bidb.personetakip.model.Excuse;
import com.bidb.personetakip.model.ExcuseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for excuse management operations.
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 8.3, 8.4, 8.5
 */
public interface ExcuseService {
    
    /**
     * Submits a new excuse request.
     * 
     * @param userId User ID submitting the excuse
     * @param excuseRequest Excuse request details
     * @return Response DTO with submission result
     * Requirements: 3.4, 3.5
     */
    ExcuseResponseDto submitExcuse(Long userId, ExcuseRequestDto excuseRequest);
    
    /**
     * Gets available excuse types.
     * 
     * @return List of excuse types
     * Requirements: 3.1, 3.2
     */
    List<ExcuseTypeDto> getExcuseTypes();
    
    /**
     * Gets excuses for a specific user.
     * 
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of user excuses
     */
    Page<Excuse> getUserExcuses(Long userId, Pageable pageable);
    
    /**
     * Gets excuses for a specific user and status.
     * 
     * @param userId User ID
     * @param status Excuse status
     * @param pageable Pagination information
     * @return Page of user excuses with specific status
     */
    Page<Excuse> getUserExcusesByStatus(Long userId, ExcuseStatus status, Pageable pageable);
    
    /**
     * Gets excuses for a specific user within a date range.
     * 
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Page of user excuses within date range
     */
    Page<Excuse> getUserExcusesByDateRange(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * Gets pending excuses for admin review.
     * 
     * @param pageable Pagination information
     * @return Page of pending excuses
     * Requirements: 8.3, 8.4
     */
    Page<Excuse> getPendingExcusesForAdmin(Pageable pageable);
    
    /**
     * Approves an excuse.
     * 
     * @param excuseId Excuse ID
     * @param adminId Admin ID performing the action
     * @param notes Optional admin notes
     * @return Updated excuse
     * Requirements: 8.4, 8.5
     */
    Excuse approveExcuse(Long excuseId, Long adminId, String notes);
    
    /**
     * Rejects an excuse.
     * 
     * @param excuseId Excuse ID
     * @param adminId Admin ID performing the action
     * @param notes Optional admin notes
     * @return Updated excuse
     * Requirements: 8.4, 8.5
     */
    Excuse rejectExcuse(Long excuseId, Long adminId, String notes);
    
    /**
     * Gets excuse by ID.
     * 
     * @param excuseId Excuse ID
     * @return Optional excuse
     */
    Optional<Excuse> getExcuseById(Long excuseId);
    
    /**
     * Checks if user has already submitted an excuse for a specific date.
     * 
     * @param userId User ID
     * @param date Excuse date
     * @return true if excuse exists for the date
     */
    boolean hasExcuseForDate(Long userId, LocalDate date);
    
    /**
     * Gets count of pending excuses for a user.
     * 
     * @param userId User ID
     * @return Count of pending excuses
     */
    long getPendingExcuseCount(Long userId);
    
    /**
     * Gets total count of pending excuses (for admin dashboard).
     * 
     * @return Total count of pending excuses
     */
    long getTotalPendingExcuseCount();
    
    /**
     * Validates excuse request.
     * 
     * @param excuseRequest Excuse request to validate
     * @return Validation error message or null if valid
     */
    String validateExcuseRequest(ExcuseRequestDto excuseRequest);
    
    /**
     * Gets excuse statistics for a user within a date range.
     * 
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @return Map of status to count
     */
    java.util.Map<ExcuseStatus, Long> getExcuseStatistics(Long userId, LocalDate startDate, LocalDate endDate);
}