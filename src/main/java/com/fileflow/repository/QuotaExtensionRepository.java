package com.fileflow.repository;

import com.fileflow.model.QuotaExtension;
import com.fileflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuotaExtensionRepository extends JpaRepository<QuotaExtension, Long> {
    /**
     * Find active quota extensions for a user
     */
    List<QuotaExtension> findByUserAndExpiryDateAfter(User user, LocalDateTime now);

    /**
     * Find expired quota extensions
     */
    List<QuotaExtension> findByExpiryDateBefore(LocalDateTime now);

    /**
     * Calculate total additional space for a user
     */
    @Query("SELECT SUM(qe.additionalSpace) FROM QuotaExtension qe WHERE qe.user = ?1 AND qe.expiryDate > ?2")
    Long calculateTotalAdditionalSpace(User user, LocalDateTime now);

    List<QuotaExtension> findByUser(User user);
}