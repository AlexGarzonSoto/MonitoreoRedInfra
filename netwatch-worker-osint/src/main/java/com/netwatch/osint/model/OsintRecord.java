package com.netwatch.osint.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro persistido de cada enriquecimiento OSINT realizado.
 * Permite auditar qué IPs fueron consultadas y qué datos se obtuvieron.
 */
@Entity
@Table(name = "osint_records",
       indexes = {
           @Index(name = "idx_osint_ip",        columnList = "ip"),
           @Index(name = "idx_osint_threat_id",  columnList = "threatId"),
           @Index(name = "idx_osint_enriched_at",columnList = "enrichedAt")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OsintRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID de la amenaza que originó este enriquecimiento (referencia lógica). */
    @Column(nullable = false)
    private UUID threatId;

    @Column(nullable = false)
    private String ip;

    private String country;
    private String city;
    private Double latitude;
    private Double longitude;

    /** Nombre del sistema autónomo (Autonomous System Number). */
    private String asn;

    /** true si la consulta a ip-api.com fue exitosa. */
    @Builder.Default
    private boolean resolved = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime enrichedAt;
}
