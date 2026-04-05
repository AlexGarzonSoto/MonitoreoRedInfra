package com.netwatch.alerts.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de cada notificación enviada por este worker.
 * Permite auditar si las alertas fueron enviadas correctamente.
 */
@Entity
@Table(name = "alert_logs",
       indexes = {
           @Index(name = "idx_log_threat_id", columnList = "threatId"),
           @Index(name = "idx_log_channel",   columnList = "channel"),
           @Index(name = "idx_log_sent_at",   columnList = "sentAt")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID de la amenaza que originó esta notificación (referencia lógica, no FK). */
    @Column(nullable = false)
    private UUID threatId;

    @Column(nullable = false)
    private String srcIp;

    @Column(nullable = false)
    private String threatType;

    @Column(nullable = false)
    private String severity;

    /** Canal por el que se envió: EMAIL o WEBHOOK. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    private boolean success;

    /** Mensaje de error si la notificación falló. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    public enum Channel {
        EMAIL, WEBHOOK
    }
}
