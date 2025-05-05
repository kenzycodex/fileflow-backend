package com.fileflow.repository;

import com.fileflow.model.Activity;
import com.fileflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByUser(User user, Pageable pageable);

    List<Activity> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    List<Activity> findByCreatedAtBefore(LocalDateTime date);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user = :user AND a.createdAt BETWEEN :start AND :end")
    long countByUserAndCreatedAtBetween(@Param("user") User user,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Activity a WHERE a.createdAt BETWEEN :start AND :end")
    List<Activity> findByCreatedAtBetween(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}