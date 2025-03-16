package com.fileflow.repository;

import com.fileflow.model.Activity;
import com.fileflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByUser(User user, Pageable pageable);

    List<Activity> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    List<Activity> findByCreatedAtBefore(LocalDateTime date);
}