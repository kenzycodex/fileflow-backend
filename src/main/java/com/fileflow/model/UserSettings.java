package com.fileflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_settings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ThemePreference themePreference;

    private boolean notificationEmail;

    private boolean notificationInApp;

    @Enumerated(EnumType.STRING)
    private DefaultView defaultView;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ThemePreference {
        LIGHT,
        DARK,
        SYSTEM
    }

    public enum DefaultView {
        GRID,
        LIST
    }
}
