package com.bidb.personetakip.repository.external;

import com.bidb.personetakip.model.ExternalTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExternalTitle entity operations.
 * This repository connects to the external read-only database.
 */
@Repository
public interface ExternalTitleRepository extends JpaRepository<ExternalTitle, String> {
    
    /**
     * Find title by code
     * @param unvkod Title code
     * @return Optional containing the title if found
     */
    Optional<ExternalTitle> findByUnvkod(String unvkod);
}
