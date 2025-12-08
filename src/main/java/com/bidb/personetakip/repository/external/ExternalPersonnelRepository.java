package com.bidb.personetakip.repository.external;

import com.bidb.personetakip.model.ExternalPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExternalPersonnel entity operations.
 * This repository connects to the external read-only database.
 */
@Repository
public interface ExternalPersonnelRepository extends JpaRepository<ExternalPersonnel, Long> {
    
    /**
     * Find personnel by TC number and personnel number
     * @param tcNo Turkish Citizen ID number
     * @param personnelNo Personnel/Employee number
     * @return Optional containing the personnel if found
     */
    Optional<ExternalPersonnel> findByTcNoAndPersonnelNo(String tcNo, String personnelNo);
    
    /**
     * Find personnel by TC number
     * @param tcNo Turkish Citizen ID number
     * @return Optional containing the personnel if found
     */
    Optional<ExternalPersonnel> findByTcNo(String tcNo);
    
    /**
     * Find personnel by personnel number
     * @param personnelNo Personnel/Employee number
     * @return Optional containing the personnel if found
     */
    Optional<ExternalPersonnel> findByPersonnelNo(String personnelNo);
}
