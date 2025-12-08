package com.bidb.personetakip.repository;

import com.bidb.personetakip.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
