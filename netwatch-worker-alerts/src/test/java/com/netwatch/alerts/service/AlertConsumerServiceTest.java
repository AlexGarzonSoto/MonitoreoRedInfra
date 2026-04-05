package com.netwatch.alerts.service;

import com.netwatch.alerts.config.AlertProperties;
import com.netwatch.alerts.dto.ThreatMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertConsumerServiceTest {

    @Mock EmailAlertService   emailService;
    @Mock WebhookAlertService webhookService;
    @Mock AlertProperties     props;
    @Mock MeterRegistry       meterRegistry;
    @Mock Counter             counter;

    @InjectMocks AlertConsumerService consumer;

    private ThreatMessage threat(String severity) {
        return new ThreatMessage(UUID.randomUUID(), "1.2.3.4", "5.6.7.8",
                55000, 22, "TCP", "BRUTE_FORCE", severity, "Desc", LocalDateTime.now());
    }

    @BeforeEach
    void stubMetrics() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    @Test
    void onAlert_criticalSeverity_dispatchesBothChannels() {
        when(props.getMinSeverity()).thenReturn("HIGH");

        consumer.onAlert(threat("CRITICAL"));

        verify(emailService).send(any());
        verify(webhookService).send(any());
    }

    @Test
    void onAlert_highSeverity_dispatchesBothChannels() {
        when(props.getMinSeverity()).thenReturn("HIGH");

        consumer.onAlert(threat("HIGH"));

        verify(emailService).send(any());
        verify(webhookService).send(any());
    }

    @Test
    void onAlert_lowSeverity_belowThreshold_doesNotDispatch() {
        when(props.getMinSeverity()).thenReturn("HIGH");

        consumer.onAlert(threat("LOW"));

        verify(emailService, never()).send(any());
        verify(webhookService, never()).send(any());
    }

    @Test
    void onAlert_mediumSeverity_belowThreshold_doesNotDispatch() {
        when(props.getMinSeverity()).thenReturn("HIGH");

        consumer.onAlert(threat("MEDIUM"));

        verify(emailService, never()).send(any());
        verify(webhookService, never()).send(any());
    }

    @Test
    void onAlert_exceptionInEmail_doesNotPropagate() {
        when(props.getMinSeverity()).thenReturn("INFO");
        doThrow(new RuntimeException("smtp error")).when(emailService).send(any());

        consumer.onAlert(threat("HIGH")); // no debe lanzar excepción

        verify(meterRegistry).counter("alerts.errors");
    }
}
