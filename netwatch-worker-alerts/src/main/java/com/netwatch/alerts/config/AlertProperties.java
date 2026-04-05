package com.netwatch.alerts.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración para los canales de notificación.
 * Se cargan desde el prefijo "netwatch.alerts" en application.properties.
 */
@ConfigurationProperties(prefix = "netwatch.alerts")
@Getter
@Setter
public class AlertProperties {

    /** Severidad mínima para enviar notificación (INFO, LOW, MEDIUM, HIGH, CRITICAL). */
    private String minSeverity = "HIGH";

    /** Email destino para notificaciones. */
    private String emailTo = "";

    /** Email remitente. */
    private String emailFrom = "netwatch@localhost";

    /** URL del webhook (Slack, Discord, Teams). Vacío = deshabilitado. */
    private String webhookUrl = "";

    /** Habilita el canal de email. */
    private boolean emailEnabled = false;

    /** Habilita el canal de webhook. */
    private boolean webhookEnabled = false;
}
