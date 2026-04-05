package com.netwatch.analysis.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje publicado en netwatch.threats.detected y netwatch.alerts.notify
 * cuando se detecta una amenaza.
 * También se publica en netwatch.osint.enrich para enriquecimiento de IP.
 */
public record ThreatMessage(
        UUID id,
        String srcIp,
        String dstIp,
        Integer srcPort,
        Integer dstPort,
        String protocol,
        String threatType,
        String severity,
        String description,
        LocalDateTime detectedAt
) {}
