package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.DepartmentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository interface for DepartmentPermission entity operations.
 */
@Repository
public interface DepartmentPermissionRepository extends JpaRepository<DepartmentPermission, Long> {
    
    /**
     * Find all department codes that a user has permission to manage.
     * 
     * @param userId User ID
     * @return List of department codes
     */
    @Query("SELECT dp.departmentCode FROM DepartmentPermission dp WHERE dp.userId = :userId")
    List<String> findDepartmentCodesByUserId(@Param("userId") Long userId);
    
    /**
     * Find all department permissions for a user.
     * 
     * @param userId User ID
     * @return List of department permissions
     */
    List<DepartmentPermission> findByUserId(Long userId);
    
    /**
     * Check if a user has permission for a specific department.
     * 
     * @param userId User ID
     * @param departmentCode Department code
     * @return true if user has permission
     */
    boolean existsByUserIdAndDepartmentCode(Long userId, String departmentCode);
    
    /**
     * Delete all permissions for a user.
     * 
     * @param userId User ID
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM DepartmentPermission dp WHERE dp.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * Delete a specific permission.
     * 
     * @param userId User ID
     * @param departmentCode Department code
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM DepartmentPermission dp WHERE dp.userId = :userId AND dp.departmentCode = :departmentCode")
    void deleteByUserIdAndDepartmentCode(@Param("userId") Long userId, @Param("departmentCode") String departmentCode);
    
    /**
     * Find all users who have permission for a specific department.
     * 
     * @param departmentCode Department code
     * @return List of user IDs
     */
    @Query("SELECT dp.userId FROM DepartmentPermission dp WHERE dp.departmentCode = :departmentCode")
    List<Long> findUserIdsByDepartmentCode(@Param("departmentCode") String departmentCode);
}