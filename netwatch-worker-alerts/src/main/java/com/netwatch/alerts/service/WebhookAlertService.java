package com.netwatch.alerts.service;

import com.netwatch.alerts.config.AlertProperties;
import com.netwatch.alerts.dto.ThreatMessage;
import com.netwatch.alerts.model.AlertLog;
import com.netwatch.alerts.repository.AlertLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envía notificaciones a un webhook HTTP (Slack, Discord, Teams…)
 * cuando se detecta una amenaza.
 * Se deshabilita con netwatch.alerts.webhook-enabled=false.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookAlertService {

    private final RestClient.Builder restClientBuilder;
    private final AlertProperties    props;
    private final AlertLogRepository logRepository;

    public void send(ThreatMessage threat) {
        if (!props.isWebhookEnabled() || props.getWebhookUrl().isBlank()) {
            log.debug("Notificación webhook deshabilitada o sin URL");
            return;
        }

        boolean success = false;
        String  error   = null;

        try {
            Map<String, String> payload = Map.of(
                    "text", "[NetWatch] %s | Severidad: %s | IP: %s | %s"
                            .formatted(threat.threatType(), threat.severity(),
                                       threat.srcIp(), threat.description())
            );

            restClientBuilder.build()
                    .post()
                    .uri(props.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            success = true;
            log.info("Webhook enviado: threatId={}", threat.id());
        } catch (Exception e) {
            error = e.getMessage();
            log.error("Error enviando webhook: {}", e.getMessage());
        }

        logRepository.save(AlertLog.builder()
                .threatId(threat.id())
                .srcIp(threat.srcIp())
                .threatType(threat.threatType())
                .severity(threat.severity())
                .channel(AlertLog.Channel.WEBHOOK)
                .success(success)
                .errorMessage(error)
                .build());
    }
}
