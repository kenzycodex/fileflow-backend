package com.fileflow.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shares")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Share {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Long itemId;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Setter
    @Getter
    @Transient
    private String itemName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Size(max = 100)
    private String recipientEmail;

    @NotBlank
    @Size(max = 255)
    private String shareLink;

    @Enumerated(EnumType.STRING)
    private Permission permissions;

    private LocalDateTime expiryDate;

    private boolean isPasswordProtected;

    @Size(max = 100)
    private String passwordHash;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private int viewCount;

    public enum ItemType {
        FILE,
        FOLDER
    }

    public enum Permission {
        VIEW,
        EDIT,
        COMMENT
    }
}
