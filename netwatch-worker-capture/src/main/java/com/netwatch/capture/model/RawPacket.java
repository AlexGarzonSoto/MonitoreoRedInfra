package com.netwatch.capture.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que persiste los metadatos de cada paquete capturado.
 * No almacena el payload completo para evitar saturar el disco;
 * el análisis profundo se delega al worker-analysis.
 */
@Entity
@Table(name = "raw_packets",
       indexes = {
           @Index(name = "idx_raw_src_ip",   columnList = "srcIp"),
           @Index(name = "idx_raw_protocol", columnList = "protocol"),
           @Index(name = "idx_raw_captured", columnList = "capturedAt")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RawPacket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String srcIp;

    private String dstIp;
    private Integer srcPort;
    private Integer dstPort;

    @Column(nullable = false)
    private String protocol;

    /** Flags TCP activos, p.ej. "SYN,ACK". Null para UDP/ICMP. */
    private String flags;

    private Integer packetLength;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime capturedAt;
}
