package com.netwatch.gateway.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // UUID directo (sin FK JPA): network_events es hypertable TimescaleDB
    // y no soporta foreign key references convencionales
    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String details;

    private boolean notificationSent = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum AlertStatus {
        OPEN, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE
    }
}