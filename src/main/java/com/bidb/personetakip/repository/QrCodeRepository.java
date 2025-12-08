package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for QrCode entity operations.
 */
@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    
    /**
     * Find QR code by user ID and valid date
     * @param userId User ID
     * @param validDate Date for which the QR code is valid
     * @return Optional containing the QR code if found
     */
    Optional<QrCode> findByUserIdAndValidDate(Long userId, LocalDate validDate);
    
    /**
     * Find QR code by its value
     * @param qrCodeValue Unique QR code value
     * @return Optional containing the QR code if found
     */
    Optional<QrCode> findByQrCodeValue(String qrCodeValue);
    
    /**
     * Find all QR codes for a user
     * @param userId User ID
     * @return List of QR codes
     */
    List<QrCode> findByUserId(Long userId);
    
    /**
     * Find all QR codes that are older than a specific date
     * @param date Cutoff date
     * @return List of old QR codes
     */
    @Query("SELECT q FROM QrCode q WHERE q.validDate < :date")
    List<QrCode> findOldQrCodes(@Param("date") LocalDate date);
    
    /**
     * Check if a QR code exists for user and date
     * @param userId User ID
     * @param validDate Valid date
     * @return true if QR code exists
     */
    boolean existsByUserIdAndValidDate(Long userId, LocalDate validDate);
    
    /**
     * Find QR codes by user ID and usage count
     * @param userId User ID
     * @param usageCount Usage count
     * @return List of QR codes
     */
    List<QrCode> findByUserIdAndUsageCount(Long userId, Integer usageCount);
}
