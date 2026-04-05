package com.netwatch.alerts.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje recibido desde la cola netwatch.alerts.notify.
 * Misma estructura que el ThreatMessage del worker-analysis.
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
