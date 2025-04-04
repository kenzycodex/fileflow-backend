package com.fileflow.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing Multi-Factor Authentication data
 */
@Entity
@Table(name = "user_mfa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMfa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "secret", nullable = false)
    private String secret;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "backup_codes")
    private String backupCodes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "recovery_email")
    private String recoveryEmail;

    @Column(name = "method")
    @Enumerated(EnumType.STRING)
    private MfaMethod method;

    public enum MfaMethod {
        TOTP,        // Time-based One-Time Password (Authenticator app)
        SMS,         // SMS-based verification
        EMAIL,       // Email-based verification
        SECURITY_KEY // WebAuthn/FIDO2 security key
    }
}