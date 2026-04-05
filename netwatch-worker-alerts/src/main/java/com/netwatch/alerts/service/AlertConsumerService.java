package com.netwatch.alerts.service;

import com.netwatch.alerts.config.AlertProperties;
import com.netwatch.alerts.config.RabbitMQConfig;
import com.netwatch.alerts.dto.ThreatMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consume mensajes de netwatch.alerts.notify y despacha
 * las notificaciones a los canales configurados.
 *
 * Filtro de severidad: solo notifica si la amenaza supera
 * netwatch.alerts.min-severity (por defecto HIGH).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConsumerService {

    private static final List<String> SEVERITY_ORDER =
            List.of("INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final EmailAlertService   emailService;
    private final WebhookAlertService webhookService;
    private final AlertProperties     props;
    private final MeterRegistry       meterRegistry;

    @RabbitListener(queues = RabbitMQConfig.ALERTS_QUEUE)
    public void onAlert(ThreatMessage threat) {
        try {
            meterRegistry.counter("alerts.received",
                    "severity", threat.severity()).increment();

            if (!meetsMinSeverity(threat.severity())) {
                log.debug("Amenaza ignorada por severidad: {} < {}",
                        threat.severity(), props.getMinSeverity());
                return;
            }

            log.info("Procesando alerta: type={} severity={} src={}",
                    threat.threatType(), threat.severity(), threat.srcIp());

            emailService.send(threat);
            webhookService.send(threat);

            meterRegistry.counter("alerts.dispatched",
                    "severity", threat.severity()).increment();

        } catch (Exception e) {
            meterRegistry.counter("alerts.errors").increment();
            log.error("Error procesando alerta: {}", e.getMessage(), e);
        }
    }

    private boolean meetsMinSeverity(String severity) {
        int min     = SEVERITY_ORDER.indexOf(props.getMinSeverity().toUpperCase());
        int current = SEVERITY_ORDER.indexOf(severity.toUpperCase());
        return current >= min;
    }
}
