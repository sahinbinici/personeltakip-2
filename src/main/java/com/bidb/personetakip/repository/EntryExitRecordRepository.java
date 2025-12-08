package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for EntryExitRecord entity operations.
 */
@Repository
public interface EntryExitRecordRepository extends JpaRepository<EntryExitRecord, Long> {
    
    /**
     * Find all entry/exit records for a user
     * @param userId User ID
     * @return List of entry/exit records
     */
    List<EntryExitRecord> findByUserId(Long userId);
    
    /**
     * Find all entry/exit records for a user within a date range
     * @param userId User ID
     * @param startDate Start date/time
     * @param endDate End date/time
     * @return List of entry/exit records
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.userId = :userId AND e.timestamp BETWEEN :startDate AND :endDate ORDER BY e.timestamp DESC")
    List<EntryExitRecord> findByUserIdAndTimestampBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find all entry/exit records by QR code value
     * @param qrCodeValue QR code value
     * @return List of entry/exit records
     */
    List<EntryExitRecord> findByQrCodeValue(String qrCodeValue);
    
    /**
     * Find all entry/exit records by type
     * @param type Entry or Exit type
     * @return List of entry/exit records
     */
    List<EntryExitRecord> findByType(EntryExitType type);
    
    /**
     * Find the most recent entry/exit record for a user
     * @param userId User ID
     * @return List containing the most recent record
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.userId = :userId ORDER BY e.timestamp DESC")
    List<EntryExitRecord> findLatestByUserId(@Param("userId") Long userId);
    
    /**
     * Count entry/exit records for a user on a specific date
     * @param userId User ID
     * @param startOfDay Start of day timestamp
     * @param endOfDay End of day timestamp
     * @return Count of records
     */
    @Query("SELECT COUNT(e) FROM EntryExitRecord e WHERE e.userId = :userId AND e.timestamp BETWEEN :startOfDay AND :endOfDay")
    long countByUserIdAndDate(
        @Param("userId") Long userId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );
}
