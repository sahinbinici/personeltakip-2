package com.bidb.personetakip.repository.external;

import com.bidb.personetakip.model.ExternalDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExternalDepartment entity operations.
 * This repository connects to the external read-only database.
 */
@Repository
public interface ExternalDepartmentRepository extends JpaRepository<ExternalDepartment, String> {
    
    /**
     * Find department by code
     * @param brkodu Department code
     * @return Optional containing the department if found
     */
    Optional<ExternalDepartment> findByBrkodu(String brkodu);
}
