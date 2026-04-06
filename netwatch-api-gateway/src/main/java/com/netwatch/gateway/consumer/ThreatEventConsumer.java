package com.netwatch.gateway.consumer;

import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.repository.AlertRepository;
import com.netwatch.gateway.repository.NetworkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consume amenazas detectadas por worker-analysis y worker-osint
 * y las persiste como NetworkEvent en la base de datos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ThreatEventConsumer {

    private final NetworkEventRepository eventRepository;
    private final AlertRepository         alertRepository;

    @RabbitListener(queues = "netwatch.threats.detected",
                    containerFactory = "rabbitListenerContainerFactory")
    public void onThreatDetected(Map<String, Object> payload) {
        try {
            NetworkEvent event = NetworkEvent.builder()
                    .srcIp(str(payload, "srcIp"))
                    .dstIp(str(payload, "dstIp"))
                    .dstPort(intVal(payload, "dstPort"))
                    .protocol(str(payload, "protocol"))
                    .threatType(parseThreatType(str(payload, "threatType")))
                    .severity(parseSeverity(str(payload, "severity")))
                    .description(str(payload, "description"))
                    .resolved(false)
                    .build();

            // Campos OSINT opcionales (presentes si pasó por worker-osint)
            if (payload.containsKey("country"))    event.setCountry(str(payload, "country"));
            if (payload.containsKey("city"))       event.setCity(str(payload, "city"));
            if (payload.containsKey("asn"))        event.setAsn(str(payload, "asn"));
            if (payload.containsKey("latitude"))   event.setLatitude(doubleVal(payload, "latitude"));
            if (payload.containsKey("longitude"))  event.setLongitude(doubleVal(payload, "longitude"));
            if (payload.containsKey("abuseScore")) event.setAbuseScore(intVal(payload, "abuseScore"));

            NetworkEvent saved = eventRepository.save(event);
            log.info("Amenaza persistida: {} {} desde {}", saved.getThreatType(), saved.getSeverity(), saved.getSrcIp());

            // Crear alerta para eventos HIGH y CRITICAL
            if (saved.getSeverity() == NetworkEvent.Severity.HIGH
                    || saved.getSeverity() == NetworkEvent.Severity.CRITICAL) {
                Alert alert = Alert.builder()
                        .eventId(saved.getId())
                        .title("[" + saved.getSeverity() + "] " + saved.getThreatType() + " desde " + saved.getSrcIp())
                        .details(saved.getDescription())
                        .status(Alert.AlertStatus.OPEN)
                        .notificationSent(false)
                        .build();
                alertRepository.save(alert);
            }

        } catch (Exception e) {
            log.error("Error persistiendo amenaza: {}", e.getMessage(), e);
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    private Double doubleVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }

    private NetworkEvent.ThreatType parseThreatType(String s) {
        if (s == null) return NetworkEvent.ThreatType.NORMAL;
        try { return NetworkEvent.ThreatType.valueOf(s); }
        catch (IllegalArgumentException e) { return NetworkEvent.ThreatType.NORMAL; }
    }

    private NetworkEvent.Severity parseSeverity(String s) {
        if (s == null) return NetworkEvent.Severity.INFO;
        try { return NetworkEvent.Severity.valueOf(s); }
        catch (IllegalArgumentException e) { return NetworkEvent.Severity.INFO; }
    }
}
