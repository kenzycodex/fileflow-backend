package com.fileflow.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User entity with enhanced security features
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "username")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_firebase_uid", columnList = "firebase_uid"),
                @Index(name = "idx_user_reset_token", columnList = "reset_password_token"),
                @Index(name = "idx_user_email_token", columnList = "email_verification_token")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 255)
    private String profileImagePath;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(length = 100)
    private String emailVerificationToken;

    @Column
    private LocalDateTime emailVerificationTokenExpiry;

    @Column(length = 255)
    private String resetPasswordToken;

    @Column
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(length = 255)
    private String firebaseUid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLogin;

    @Column(length = 50)
    private String lastLoginIp;

    @Column(length = 50)
    private String registrationIp;

    @Column
    private LocalDateTime lastUsernameChange;

    @Column
    private LocalDateTime passwordUpdatedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private Long storageQuota;

    @Column(nullable = false)
    private Long storageUsed;

    @Column
    private Long storageLimit;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(length = 100)
    private String lastLogoutTime;

    /**
     * User status enum
     */
    public enum Status {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        DELETED
    }

    /**
     * Authentication provider enum
     */
    public enum AuthProvider {
        LOCAL,
        GOOGLE,
        GITHUB,
        MICROSOFT,
        APPLE
    }

    /**
     * User role enum
     */
    public enum UserRole {
        USER,
        ADMIN
    }

    /**
     * Pre-update callback to set the updated timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}