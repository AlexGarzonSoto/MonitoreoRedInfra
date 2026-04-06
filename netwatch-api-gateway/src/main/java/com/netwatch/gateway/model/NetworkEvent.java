package com.netwatch.gateway.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "network_events",
       indexes = {
           @Index(name = "idx_src_ip", columnList = "srcIp"),
           @Index(name = "idx_timestamp", columnList = "timestamp"),
           @Index(name = "idx_severity", columnList = "severity")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NetworkEvent {

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
    private Integer packetLength;

    @Enumerated(EnumType.STRING)
    private ThreatType threatType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String country;
    private String city;
    private String asn;
    private Double latitude;
    private Double longitude;
    private Integer abuseScore;

    private boolean resolved = false;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public enum ThreatType {
        PORT_SCAN, BRUTE_FORCE, SYN_FLOOD, DNS_TUNNELING,
        DATA_EXFILTRATION, MALWARE_C2, NORMAL
    }

    public enum Severity {
        INFO, LOW, MEDIUM, HIGH, CRITICAL
    }
}