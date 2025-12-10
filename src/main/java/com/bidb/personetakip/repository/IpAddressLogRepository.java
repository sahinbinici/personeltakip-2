package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.IpAddressAction;
import com.bidb.personetakip.model.IpAddressLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for IP address audit log operations.
 * Provides methods for querying and managing IP address access logs.
 */
@Repository
public interface IpAddressLogRepository extends JpaRepository<IpAddressLog, Long> {
    
    /**
     * Find all IP address logs for a specific user
     * @param userId the user ID to search for
     * @param pageable pagination information
     * @return page of IP address logs for the user
     */
    Page<IpAddressLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    /**
     * Find all IP address logs by action type
     * @param action the action type to search for
     * @param pageable pagination information
     * @return page of IP address logs with the specified action
     */
    Page<IpAddressLog> findByActionOrderByTimestampDesc(IpAddressAction action, Pageable pageable);
    
    /**
     * Find all IP address logs within a time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @param pageable pagination information
     * @return page of IP address logs within the time range
     */
    Page<IpAddressLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startTime, 
        LocalDateTime endTime, 
        Pageable pageable
    );
    
    /**
     * Find all IP address logs for a specific IP address
     * @param ipAddress the IP address to search for
     * @param pageable pagination information
     * @return page of IP address logs for the IP address
     */
    Page<IpAddressLog> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);
    
    /**
     * Find all IP address logs performed by a specific admin user
     * @param adminUserId the admin user ID to search for
     * @param pageable pagination information
     * @return page of IP address logs performed by the admin user
     */
    Page<IpAddressLog> findByAdminUserIdOrderByTimestampDesc(Long adminUserId, Pageable pageable);
    
    /**
     * Count IP address logs by action type within a time range
     * @param action the action type to count
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of logs matching the criteria
     */
    @Query("SELECT COUNT(l) FROM IpAddressLog l WHERE l.action = :action AND l.timestamp BETWEEN :startTime AND :endTime")
    long countByActionAndTimestampBetween(
        @Param("action") IpAddressAction action,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Find recent IP address logs for a user (last 30 days)
     * @param userId the user ID to search for
     * @return list of recent IP address logs
     */
    @Query("SELECT l FROM IpAddressLog l WHERE l.userId = :userId AND l.timestamp >= :since ORDER BY l.timestamp DESC")
    List<IpAddressLog> findRecentLogsByUserId(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );
}