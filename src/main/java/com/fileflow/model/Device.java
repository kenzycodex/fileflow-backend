package com.fileflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "devices")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 100)
    private String deviceName;

    @NotBlank
    @Size(max = 50)
    private String deviceType;

    @NotBlank
    @Size(max = 50)
    private String platform;

    private LocalDateTime lastSyncDate;

    @Size(max = 255)
    private String pushToken;

    private LocalDateTime created_at;

    private LocalDateTime lastActive;
}
