package com.netwatch.gateway.dto;

import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.model.NetworkEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de alerta que incluye un resumen del evento asociado.
 * Permite que el frontend acceda a alert.event.srcIp, alert.event.severity, etc.
 */
public record AlertDTO(
        UUID id,
        LocalDateTime createdAt,
        String title,
        String details,
        String status,
        boolean notificationSent,
        UUID eventId,
        EventSummary event
) {

    public record EventSummary(
            UUID id,
            String srcIp,
            String dstIp,
            Integer srcPort,
            Integer dstPort,
            String severity,
            String threatType,
            String country,
            LocalDateTime timestamp
    ) {
        public static EventSummary from(NetworkEvent e) {
            if (e == null) return null;
            return new EventSummary(
                    e.getId(),
                    e.getSrcIp(),
                    e.getDstIp(),
                    e.getSrcPort(),
                    e.getDstPort(),
                    e.getSeverity()   != null ? e.getSeverity().name()   : null,
                    e.getThreatType() != null ? e.getThreatType().name() : null,
                    e.getCountry(),
                    e.getTimestamp()
            );
        }
    }

    public static AlertDTO from(Alert alert, NetworkEvent event) {
        return new AlertDTO(
                alert.getId(),
                alert.getCreatedAt(),
                alert.getTitle(),
                alert.getDetails(),
                alert.getStatus() != null ? alert.getStatus().name() : null,
                alert.isNotificationSent(),
                alert.getEventId(),
                EventSummary.from(event)
        );
    }
}
