package com.bidb.personetakip.repository.external;

import com.bidb.personetakip.model.ExternalPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExternalPersonnel entity operations.
 * This repository connects to the external read-only database.
 * 
 * External DB: 193.140.136.45
 * Database: isicil
 */
@Repository
public interface ExternalPersonnelRepository extends JpaRepository<ExternalPersonnel, Long> {
    
    /**
     * Find personnel by TC number and personnel number
     * @param tckiml Turkish Citizen ID number
     * @param esicno Personnel/Employee number
     * @return Optional containing the personnel if found
     */
    Optional<ExternalPersonnel> findByTckimlAndEsicno(String tckiml, Long esicno);
    
    /**
     * Find personnel by TC number
     * @param tckiml Turkish Citizen ID number
     * @return Optional containing the personnel if found
     */
    Optional<ExternalPersonnel> findByTckiml(String tckiml);
    
    /**
     * Find personnel by personnel number
     * @param esicno Personnel/Employee number
     * @return Optional containing the personnel if found
     */
    Optional<ExternalPersonnel> findByEsicno(Long esicno);
    
    // Convenience methods using aliases
    default Optional<ExternalPersonnel> findByTcNoAndPersonnelNo(String tcNo, String personnelNo) {
        try {
            Long esicno = Long.parseLong(personnelNo);
            return findByTckimlAndEsicno(tcNo, esicno);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    
    default Optional<ExternalPersonnel> findByTcNo(String tcNo) {
        return findByTckiml(tcNo);
    }
    
    default Optional<ExternalPersonnel> findByPersonnelNo(String personnelNo) {
        try {
            Long esicno = Long.parseLong(personnelNo);
            return findByEsicno(esicno);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
