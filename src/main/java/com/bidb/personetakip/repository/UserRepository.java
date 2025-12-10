package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find a user by TC number
     * @param tcNo Turkish Citizen ID number
     * @return Optional containing the user if found
     */
    Optional<User> findByTcNo(String tcNo);
    
    /**
     * Check if a user exists with the given TC number
     * @param tcNo Turkish Citizen ID number
     * @return true if user exists
     */
    boolean existsByTcNo(String tcNo);
    
    /**
     * Find a user by personnel number
     * @param personnelNo Personnel/Employee number
     * @return Optional containing the user if found
     */
    Optional<User> findByPersonnelNo(String personnelNo);
    
    /**
     * Count users by role
     * @param role User role (NORMAL_USER, ADMIN, SUPER_ADMIN)
     * @return Number of users with the specified role
     */
    long countByRole(UserRole role);
    
    /**
     * Find users by role with pagination
     * @param role User role
     * @param pageable Pagination information
     * @return Page of users with the specified role
     */
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    /**
     * Search users by TC number, name, or personnel number
     * @param searchTerm Search term
     * @param pageable Pagination information
     * @return Page of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "u.tcNo LIKE %:searchTerm% OR " +
           "u.personnelNo LIKE %:searchTerm% OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find users by department code with pagination
     * @param departmentCode Department code
     * @param pageable Pagination information
     * @return Page of users with the specified department code
     */
    Page<User> findByDepartmentCode(String departmentCode, Pageable pageable);
    
    /**
     * Find users by role and department code with pagination
     * @param role User role
     * @param departmentCode Department code
     * @param pageable Pagination information
     * @return Page of users with the specified role and department code
     */
    Page<User> findByRoleAndDepartmentCode(UserRole role, String departmentCode, Pageable pageable);
    
    // IP Assignment Management Methods
    
    /**
     * Find users who have assigned IP addresses
     * @param pageable Pagination information
     * @return Page of users with assigned IP addresses
     */
    @Query("SELECT u FROM User u WHERE u.assignedIpAddresses IS NOT NULL AND u.assignedIpAddresses != ''")
    Page<User> findUsersWithAssignedIpAddresses(Pageable pageable);
    
    /**
     * Find users who have a specific IP address assigned
     * @param ipAddress IP address to search for
     * @return List of users with the specified IP address assigned
     */
    @Query("SELECT u FROM User u WHERE u.assignedIpAddresses LIKE %:ipAddress%")
    List<User> findByAssignedIpAddress(@Param("ipAddress") String ipAddress);
    
    /**
     * Count users who have assigned IP addresses
     * @return Number of users with assigned IP addresses
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.assignedIpAddresses IS NOT NULL AND u.assignedIpAddresses != ''")
    long countUsersWithAssignedIpAddresses();

}
