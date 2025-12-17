package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.Excuse;
import com.bidb.personetakip.model.ExcuseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Excuse entity operations.
 * Requirements: 3.4, 3.5, 8.3, 8.4, 8.5
 */
@Repository
public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    
    /**
     * Finds all excuses for a specific user.
     * 
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of excuses
     */
    Page<Excuse> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Finds excuses by user ID and status.
     * 
     * @param userId User ID
     * @param status Excuse status
     * @param pageable Pagination information
     * @return Page of excuses
     */
    Page<Excuse> findByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, ExcuseStatus status, Pageable pageable);
    
    /**
     * Finds excuses by status.
     * 
     * @param status Excuse status
     * @param pageable Pagination information
     * @return Page of excuses
     */
    Page<Excuse> findByStatusOrderBySubmittedAtDesc(ExcuseStatus status, Pageable pageable);
    
    /**
     * Finds excuses for a specific date range.
     * 
     * @param userId User ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination information
     * @return Page of excuses
     */
    Page<Excuse> findByUserIdAndExcuseDateBetweenOrderByExcuseDateDesc(
        Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * Finds pending excuses for admin review.
     * For department-based filtering, this would need to be enhanced with user department information.
     * 
     * @param pageable Pagination information
     * @return Page of pending excuses
     */
    @Query("SELECT e FROM Excuse e WHERE e.status = 'PENDING' ORDER BY e.submittedAt ASC")
    Page<Excuse> findPendingExcusesForAdmin(Pageable pageable);
    
    /**
     * Counts pending excuses for a user.
     * 
     * @param userId User ID
     * @return Count of pending excuses
     */
    long countByUserIdAndStatus(Long userId, ExcuseStatus status);
    
    /**
     * Counts total pending excuses (for admin dashboard).
     * 
     * @return Count of all pending excuses
     */
    long countByStatus(ExcuseStatus status);
    
    /**
     * Finds excuses submitted within a time range.
     * 
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of excuses
     */
    List<Excuse> findBySubmittedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Checks if user has already submitted an excuse for a specific date.
     * 
     * @param userId User ID
     * @param excuseDate Excuse date
     * @return Optional excuse if exists
     */
    Optional<Excuse> findByUserIdAndExcuseDate(Long userId, LocalDate excuseDate);
    
    /**
     * Finds excuses by excuse type.
     * 
     * @param excuseTypeId Excuse type ID
     * @param pageable Pagination information
     * @return Page of excuses
     */
    Page<Excuse> findByExcuseTypeIdOrderBySubmittedAtDesc(Long excuseTypeId, Pageable pageable);
    
    /**
     * Gets excuse statistics for a user within a date range.
     * 
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of status counts
     */
    @Query("SELECT e.status, COUNT(e) FROM Excuse e " +
           "WHERE e.userId = :userId AND e.excuseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.status")
    List<Object[]> getExcuseStatistics(@Param("userId") Long userId, 
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);
    
    /**
     * Finds recently submitted excuses (last 24 hours) for notification purposes.
     * 
     * @param since Timestamp to search from
     * @return List of recent excuses
     */
    @Query("SELECT e FROM Excuse e WHERE e.submittedAt >= :since AND e.status = 'PENDING' ORDER BY e.submittedAt DESC")
    List<Excuse> findRecentPendingExcuses(@Param("since") LocalDateTime since);
    
    /**
     * Deletes old excuses beyond retention period.
     * This method should be used carefully and typically in scheduled cleanup tasks.
     * 
     * @param beforeDate Date before which excuses should be deleted
     * @return Number of deleted records
     */
    @Query("DELETE FROM Excuse e WHERE e.submittedAt < :beforeDate")
    int deleteExcusesOlderThan(@Param("beforeDate") LocalDateTime beforeDate);
}