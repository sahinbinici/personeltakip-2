package com.bidb.personetakip.repository.external;

import com.bidb.personetakip.dto.ExternalPersonnelFullDto;
import com.bidb.personetakip.model.ExternalPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * Find complete personnel data with all JOINs using native SQL query
     * This query joins person, telefo, brkodu, and unvkod tables in a single query
     * 
     * @param tckiml Turkish Citizen ID number
     * @param esicno Personnel/Employee number
     * @return Optional containing the complete personnel data if found
     */
    @Query(value = "SELECT p.esicno AS esicno, p.tckiml AS tckiml, p.peradi AS peradi, " +
                   "p.soyadi AS soyadi, p.brkodu AS brkodu, b.BRKDAC AS brkdac, " +
                   "p.unvkod AS unvkod, u.unvack AS unvack, t.telefo AS telefo " +
                   "FROM person p " +
                   "LEFT JOIN unvkod u ON p.unvkod = u.unvkod " +
                   "LEFT JOIN brkodu b ON p.brkodu = b.BRKODU " +
                   "LEFT JOIN telefo t ON p.esicno = t.esicno " +
                   "WHERE p.tckiml = :tckiml AND p.esicno = :esicno",
           nativeQuery = true)
    Optional<ExternalPersonnelFullDto> findCompletePersonnelData(
        @Param("tckiml") String tckiml, 
        @Param("esicno") Long esicno
    );
    
    /**
     * Find complete personnel data by TC number only
     * 
     * @param tckiml Turkish Citizen ID number
     * @return Optional containing the complete personnel data if found
     */
    @Query(value = "SELECT p.esicno AS esicno, p.tckiml AS tckiml, p.peradi AS peradi, " +
                   "p.soyadi AS soyadi, p.brkodu AS brkodu, b.BRKDAC AS brkdac, " +
                   "p.unvkod AS unvkod, u.unvack AS unvack, t.telefo AS telefo " +
                   "FROM person p " +
                   "LEFT JOIN unvkod u ON p.unvkod = u.unvkod " +
                   "LEFT JOIN brkodu b ON p.brkodu = b.BRKODU " +
                   "LEFT JOIN telefo t ON p.esicno = t.esicno " +
                   "WHERE p.tckiml = :tckiml " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<ExternalPersonnelFullDto> findCompletePersonnelDataByTcNo(@Param("tckiml") String tckiml);
    
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
