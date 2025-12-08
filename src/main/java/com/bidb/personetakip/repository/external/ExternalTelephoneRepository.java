package com.bidb.personetakip.repository.external;

import com.bidb.personetakip.model.ExternalTelephone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExternalTelephone entity operations.
 * This repository connects to the external read-only database.
 */
@Repository
public interface ExternalTelephoneRepository extends JpaRepository<ExternalTelephone, Long> {
    
    /**
     * Find telephone by personnel number
     * @param esicno Personnel/Employee number
     * @return Optional containing the telephone if found
     */
    Optional<ExternalTelephone> findFirstByEsicno(Long esicno);
}
