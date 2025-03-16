package com.fileflow.repository;

import com.fileflow.model.Tag;
import com.fileflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByUser(User user);

    Optional<Tag> findByUserAndName(User user, String name);

    boolean existsByUserAndName(User user, String name);
}