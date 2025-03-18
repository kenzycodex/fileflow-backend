package com.fileflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "activities")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 50)
    @Column(name = "activity_type", nullable = false)
    private String activityType;

    private Long itemId;

    @Size(max = 20)
    @Column(name = "item_type")
    private String itemType;

    @Size(max = 255)
    private String description;

    @Size(max = 50)
    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Size(max = 255)
    private String deviceInfo;
}