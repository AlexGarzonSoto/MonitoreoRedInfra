package com.netwatch.analysis.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro persistido en PostgreSQL de cada amenaza detectada por el motor STRIDE.
 */
@Entity
@Table(name = "threat_events",
       indexes = {
           @Index(name = "idx_threat_src_ip",   columnList = "srcIp"),
           @Index(name = "idx_threat_type",      columnList = "threatType"),
           @Index(name = "idx_threat_severity",  columnList = "severity"),
           @Index(name = "idx_threat_detected",  columnList = "detectedAt")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThreatEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String srcIp;

    private String dstIp;
    private Integer srcPort;
    private Integer dstPort;
    private String protocol;
    private String flags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreatType threatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** true si ya se envió notificación al worker-alerts */
    @Builder.Default
    private boolean notified = false;

    /** true si ya se enriqueció con OSINT */
    @Builder.Default
    private boolean enriched = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    public enum ThreatType {
        PORT_SCAN, BRUTE_FORCE, SYN_FLOOD, DNS_TUNNELING,
        DATA_EXFILTRATION, MALWARE_C2, NORMAL
    }

    public enum Severity {
        INFO, LOW, MEDIUM, HIGH, CRITICAL
    }
}
