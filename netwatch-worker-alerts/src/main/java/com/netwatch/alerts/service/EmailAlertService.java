package com.netwatch.alerts.service;

import com.netwatch.alerts.config.AlertProperties;
import com.netwatch.alerts.dto.ThreatMessage;
import com.netwatch.alerts.model.AlertLog;
import com.netwatch.alerts.repository.AlertLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía notificaciones por correo SMTP cuando se detecta una amenaza.
 * Se deshabilita con netwatch.alerts.email-enabled=false.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAlertService {

    private final JavaMailSender     mailSender;
    private final AlertProperties    props;
    private final AlertLogRepository logRepository;

    public void send(ThreatMessage threat) {
        if (!props.isEmailEnabled() || props.getEmailTo().isBlank()) {
            log.debug("Notificación email deshabilitada o sin destinatario");
            return;
        }

        boolean success = false;
        String  error   = null;

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getEmailFrom());
            msg.setTo(props.getEmailTo());
            msg.setSubject(buildSubject(threat));
            msg.setText(buildBody(threat));
            mailSender.send(msg);
            success = true;
            log.info("Email enviado: threatId={} severity={}", threat.id(), threat.severity());
        } catch (Exception e) {
            error = e.getMessage();
            log.error("Error enviando email: {}", e.getMessage());
        }

        logRepository.save(AlertLog.builder()
                .threatId(threat.id())
                .srcIp(threat.srcIp())
                .threatType(threat.threatType())
                .severity(threat.severity())
                .channel(AlertLog.Channel.EMAIL)
                .success(success)
                .errorMessage(error)
                .build());
    }

    private String buildSubject(ThreatMessage t) {
        return "[NetWatch] %s — Severidad %s — %s".formatted(t.threatType(), t.severity(), t.srcIp());
    }

    private String buildBody(ThreatMessage t) {
        return """
                NetWatch — Alerta de Seguridad
                ==============================
                Tipo de amenaza : %s
                Severidad       : %s
                IP origen       : %s
                IP destino      : %s
                Puerto destino  : %s
                Protocolo       : %s
                Descripción     : %s
                Detectado       : %s
                """.formatted(
                t.threatType(), t.severity(),
                t.srcIp(), t.dstIp(), t.dstPort(),
                t.protocol(), t.description(), t.detectedAt());
    }
}
