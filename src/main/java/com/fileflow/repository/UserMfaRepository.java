package com.fileflow.repository;

import com.fileflow.model.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserMfa entity
 */
@Repository
public interface UserMfaRepository extends JpaRepository<UserMfa, Long> {

    /**
     * Find MFA settings by user ID
     */
    Optional<UserMfa> findByUserId(Long userId);

    /**
     * Delete MFA settings by user ID
     */
    void deleteByUserId(Long userId);
}