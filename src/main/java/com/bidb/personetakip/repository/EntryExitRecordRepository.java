package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    /**
     * Count records by type within date range
     * @param type Entry or Exit type
     * @param startDate Start date/time
     * @param endDate End date/time
     * @return Count of records
     */
    @Query("SELECT COUNT(e) FROM EntryExitRecord e WHERE e.type = :type AND e.timestamp BETWEEN :startDate AND :endDate")
    long countByTypeAndTimestampBetween(
        @Param("type") EntryExitType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Count all records within date range
     * @param startDate Start date/time
     * @param endDate End date/time
     * @return Count of records
     */
    @Query("SELECT COUNT(e) FROM EntryExitRecord e WHERE e.timestamp BETWEEN :startDate AND :endDate")
    long countByTimestampBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find recent records after a specific timestamp
     * @param timestamp Timestamp to search after
     * @return List of recent records (max 10)
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.timestamp > :timestamp ORDER BY e.timestamp DESC")
    List<EntryExitRecord> findTop10ByTimestampAfterOrderByTimestampDesc(@Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Find all entry/exit records with pagination
     * @param pageable Pagination information
     * @return Page of entry/exit records
     */
    Page<EntryExitRecord> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * Find entry/exit records by date range with pagination
     * @param startDate Start date/time
     * @param endDate End date/time
     * @param pageable Pagination information
     * @return Page of entry/exit records within date range
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.timestamp BETWEEN :startDate AND :endDate ORDER BY e.timestamp DESC")
    Page<EntryExitRecord> findByTimestampBetweenOrderByTimestampDesc(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Find entry/exit records by user with pagination
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of entry/exit records for specified user
     */
    Page<EntryExitRecord> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    /**
     * Find entry/exit records by IP address
     * @param ipAddress IP address to filter by
     * @return List of entry/exit records with matching IP address
     */
    List<EntryExitRecord> findByIpAddress(String ipAddress);
    
    /**
     * Find entry/exit records by IP address with pagination
     * @param ipAddress IP address to filter by
     * @param pageable Pagination information
     * @return Page of entry/exit records with matching IP address
     */
    Page<EntryExitRecord> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);
    
    /**
     * Find entry/exit records with null or empty IP address
     * @return List of entry/exit records with unknown IP addresses
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.ipAddress IS NULL OR e.ipAddress = ''")
    List<EntryExitRecord> findByUnknownIpAddress();
    
    /**
     * Find entry/exit records with null or empty IP address with pagination
     * @param pageable Pagination information
     * @return Page of entry/exit records with unknown IP addresses
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.ipAddress IS NULL OR e.ipAddress = '' ORDER BY e.timestamp DESC")
    Page<EntryExitRecord> findByUnknownIpAddressOrderByTimestampDesc(Pageable pageable);
    
    /**
     * Find entry/exit records by IP address pattern (for IP range filtering)
     * @param ipPattern IP pattern to match (e.g., "192.168.1.%" for subnet)
     * @return List of entry/exit records with matching IP pattern
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.ipAddress LIKE :ipPattern")
    List<EntryExitRecord> findByIpAddressPattern(@Param("ipPattern") String ipPattern);
    
    /**
     * Find entry/exit records by IP address pattern with pagination
     * @param ipPattern IP pattern to match (e.g., "192.168.1.%" for subnet)
     * @param pageable Pagination information
     * @return Page of entry/exit records with matching IP pattern
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.ipAddress LIKE :ipPattern ORDER BY e.timestamp DESC")
    Page<EntryExitRecord> findByIpAddressPatternOrderByTimestampDesc(@Param("ipPattern") String ipPattern, Pageable pageable);
    
    /**
     * Find entry/exit records by user and IP address
     * @param userId User ID
     * @param ipAddress IP address to filter by
     * @return List of entry/exit records for user with matching IP address
     */
    List<EntryExitRecord> findByUserIdAndIpAddress(Long userId, String ipAddress);
    
    /**
     * Count entry/exit records by IP address
     * @param ipAddress IP address to count
     * @return Count of records with matching IP address
     */
    long countByIpAddress(String ipAddress);
    
    /**
     * Find distinct IP addresses used in entry/exit records
     * @return List of distinct IP addresses
     */
    @Query("SELECT DISTINCT e.ipAddress FROM EntryExitRecord e WHERE e.ipAddress IS NOT NULL AND e.ipAddress != '' ORDER BY e.ipAddress")
    List<String> findDistinctIpAddresses();
    
    /**
     * Find entry/exit records by timestamp range
     * @param startDate Start date/time
     * @param endDate End date/time
     * @return List of entry/exit records within date range
     */
    @Query("SELECT e FROM EntryExitRecord e WHERE e.timestamp BETWEEN :startDate AND :endDate ORDER BY e.timestamp DESC")
    List<EntryExitRecord> findByTimestampBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
