package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OtpVerification entity operations.
 */
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    
    /**
     * Find the most recent unverified OTP for a given TC number
     * @param tcNo Turkish Citizen ID number
     * @return Optional containing the OTP verification if found
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.tcNo = :tcNo AND o.verified = false ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findLatestUnverifiedByTcNo(@Param("tcNo") String tcNo);
    
    /**
     * Find all unverified OTPs for a given TC number
     * @param tcNo Turkish Citizen ID number
     * @return List of unverified OTP verifications
     */
    List<OtpVerification> findByTcNoAndVerifiedFalse(String tcNo);
    
    /**
     * Find OTP by TC number and OTP code
     * @param tcNo Turkish Citizen ID number
     * @param otpCode OTP code
     * @return Optional containing the OTP verification if found
     */
    Optional<OtpVerification> findByTcNoAndOtpCode(String tcNo, String otpCode);
    
    /**
     * Delete all expired OTPs
     * @param now Current timestamp
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now")
    int deleteExpiredOtps(@Param("now") LocalDateTime now);
    
    /**
     * Find all expired OTPs
     * @param now Current timestamp
     * @return List of expired OTP verifications
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.expiresAt < :now")
    List<OtpVerification> findExpiredOtps(@Param("now") LocalDateTime now);
}
