package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.QrCodeDto;
import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

@Service
public class QrCodeServiceImpl implements QrCodeService {
    
    private static final int MAX_USAGE = 2;
    private static final int QR_CODE_SIZE = 300;
    private final QrCodeRepository qrCodeRepository;
    private final SecureRandom secureRandom;
    
    public QrCodeServiceImpl(QrCodeRepository qrCodeRepository) {
        this.qrCodeRepository = qrCodeRepository;
        this.secureRandom = new SecureRandom();
    }
    
    @Override
    @Transactional
    public QrCodeDto getDailyQrCode(Long userId) {
        LocalDate today = LocalDate.now();
        
        // Try to find existing QR code for today
        Optional<QrCode> existingQrCode = qrCodeRepository.findByUserIdAndValidDate(userId, today);
        
        if (existingQrCode.isPresent()) {
            QrCode qrCode = existingQrCode.get();
            return new QrCodeDto(
                qrCode.getQrCodeValue(),
                qrCode.getValidDate(),
                qrCode.getUsageCount(),
                MAX_USAGE
            );
        }
        
        // Generate new QR code
        String qrCodeValue = generateUniqueQrCodeValue(userId, today);
        
        QrCode qrCode = new QrCode();
        qrCode.setUserId(userId);
        qrCode.setQrCodeValue(qrCodeValue);
        qrCode.setValidDate(today);
        qrCode.setUsageCount(0);
        
        qrCode = qrCodeRepository.save(qrCode);
        
        return new QrCodeDto(
            qrCode.getQrCodeValue(),
            qrCode.getValidDate(),
            qrCode.getUsageCount(),
            MAX_USAGE
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public QrCodeValidationDto validateQrCode(String qrCodeValue, Long userId) {
        Optional<QrCode> qrCodeOpt = qrCodeRepository.findByQrCodeValue(qrCodeValue);
        
        if (qrCodeOpt.isEmpty()) {
            return new QrCodeValidationDto(false, "QR code not found", null);
        }
        
        QrCode qrCode = qrCodeOpt.get();
        
        // Check ownership
        if (!qrCode.getUserId().equals(userId)) {
            return new QrCodeValidationDto(false, "QR code does not belong to user", null);
        }
        
        // Check date validity
        LocalDate today = LocalDate.now();
        if (!qrCode.getValidDate().equals(today)) {
            return new QrCodeValidationDto(false, "QR code is not valid for today", null);
        }
        
        // Check usage count
        if (qrCode.getUsageCount() >= MAX_USAGE) {
            return new QrCodeValidationDto(false, "QR code has already been used twice", null);
        }
        
        // Determine next type
        EntryExitType nextType = qrCode.getUsageCount() == 0 ? EntryExitType.ENTRY : EntryExitType.EXIT;
        
        return new QrCodeValidationDto(true, "QR code is valid", nextType);
    }
    
    @Override
    @Transactional
    public void incrementUsageCount(String qrCodeValue) {
        QrCode qrCode = qrCodeRepository.findByQrCodeValue(qrCodeValue)
            .orElseThrow(() -> new ValidationException("QR code not found"));
        
        if (qrCode.getUsageCount() >= MAX_USAGE) {
            throw new ValidationException("QR code usage limit exceeded");
        }
        
        qrCode.setUsageCount(qrCode.getUsageCount() + 1);
        qrCodeRepository.save(qrCode);
    }
    
    @Override
    public byte[] generateQrCodeImage(String qrCodeValue) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeValue,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE
            );
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
            
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }
    
    /**
     * Generates a unique QR code value using userId, date, and random salt
     */
    private String generateUniqueQrCodeValue(Long userId, LocalDate date) {
        try {
            // Generate random salt
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);
            String saltString = Base64.getEncoder().encodeToString(salt);
            
            // Combine userId, date, and salt
            String input = userId + "-" + date.toString() + "-" + saltString;
            
            // Hash with SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            
            // Encode to Base64 and return
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
