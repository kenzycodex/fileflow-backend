package com.fileflow.repository;

import com.fileflow.model.QuotaExtension;
import com.fileflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuotaExtensionRepository extends JpaRepository<QuotaExtension, Long> {
    List<QuotaExtension> findByUser(User user);

    List<QuotaExtension> findByUserAndExpiryDateAfter(User user, LocalDateTime date);
}