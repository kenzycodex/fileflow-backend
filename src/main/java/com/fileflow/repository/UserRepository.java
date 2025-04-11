package com.fileflow.repository;

import com.fileflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for User entity
 * Optimized queries for better performance
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Check if a username exists
     * @param username Username to check
     * @return Whether the username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email exists
     * @param email Email to check
     * @return Whether the email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by username (optimized single query)
     * @param username Username to search
     * @return Optional user
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email (optimized single query)
     * @param email Email to search
     * @return Optional user
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     * Only use when you need to search by both fields
     * @param username Username to search
     * @param email Email to search
     * @return Optional user
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    /**
     * Find user by email verification token
     * @param token Email verification token
     * @return Optional user
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * Find user by password reset token
     * @param token Password reset token
     * @return Optional user
     */
    Optional<User> findByResetPasswordToken(String token);

    /**
     * Find user by Firebase UID
     * @param firebaseUid Firebase UID
     * @return Optional user
     */
    Optional<User> findByFirebaseUid(String firebaseUid);

    Long countByLastLoginAfter(LocalDateTime date);
}