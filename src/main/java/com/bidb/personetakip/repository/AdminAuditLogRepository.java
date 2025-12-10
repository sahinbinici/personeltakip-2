package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AdminAuditLog entity.
 * Provides methods for querying audit logs with filtering and pagination.
 * 
 * Requirements: 4.3 - Audit logging for administrative actions
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    
    /**
     * Find audit logs by admin user ID with pagination
     */
    Page<AdminAuditLog> findByAdminUserIdOrderByTimestampDesc(Long adminUserId, Pageable pageable);
    
    /**
     * Find audit logs by target user ID with pagination
     */
    Page<AdminAuditLog> findByTargetUserIdOrderByTimestampDesc(Long targetUserId, Pageable pageable);
    
    /**
     * Find audit logs by action type with pagination
     */
    Page<AdminAuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
    
    /**
     * Find audit logs within date range with pagination
     */
    @Query("SELECT a FROM AdminAuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AdminAuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate, 
                                               Pageable pageable);
    
    /**
     * Find recent audit logs (last 24 hours) for dashboard
     */
    @Query("SELECT a FROM AdminAuditLog a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AdminAuditLog> findRecentLogs(@Param("since") LocalDateTime since);
    
    /**
     * Count audit logs by action type within date range
     */
    @Query("SELECT COUNT(a) FROM AdminAuditLog a WHERE a.action = :action AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countByActionAndTimestampBetween(@Param("action") String action, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
}