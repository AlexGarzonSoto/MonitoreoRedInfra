package com.netwatch.analysis.service;

import com.netwatch.analysis.dto.PacketMessage;
import com.netwatch.analysis.model.ThreatEvent;
import com.netwatch.analysis.repository.ThreatEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacketConsumerServiceTest {

    @Mock ThreatDetectionService detectionService;
    @Mock ThreatEventRepository  repository;
    @Mock RabbitTemplate         rabbitTemplate;
    @Mock MeterRegistry          meterRegistry;
    @Mock Counter                counter;

    @InjectMocks PacketConsumerService consumer;

    private final PacketMessage normalPacket = new PacketMessage(
            "1.2.3.4", "5.6.7.8", 55000, 8080, "TCP", "ACK", 200, LocalDateTime.now());

    @BeforeEach
    void stubMetrics() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    @Test
    void onPacket_noThreat_doesNotPublish() {
        when(detectionService.analyze(normalPacket)).thenReturn(Optional.empty());

        consumer.onPacket(normalPacket);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(repository, never()).save(any());
    }

    @Test
    void onPacket_threatDetected_savesAndPublishesTwice() {
        ThreatDetectionService.DetectionResult result = new ThreatDetectionService.DetectionResult(
                ThreatEvent.ThreatType.SYN_FLOOD,
                ThreatEvent.Severity.HIGH,
                "SYN flood detectado");

        when(detectionService.analyze(normalPacket)).thenReturn(Optional.of(result));

        ThreatEvent saved = ThreatEvent.builder()
                .srcIp("1.2.3.4").dstIp("5.6.7.8")
                .protocol("TCP")
                .threatType(ThreatEvent.ThreatType.SYN_FLOOD)
                .severity(ThreatEvent.Severity.HIGH)
                .description("SYN flood detectado")
                .build();
        when(repository.save(any())).thenReturn(saved);

        consumer.onPacket(normalPacket);

        // Debe publicar en alerts.notify y en osint.enrich
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("netwatch.direct"), anyString(), any(Object.class));
        verify(repository).save(any());
    }

    @Test
    void onPacket_exceptionInDetection_doesNotPropagate() {
        when(detectionService.analyze(any())).thenThrow(new RuntimeException("fallo"));

        consumer.onPacket(normalPacket); // no debe lanzar excepción

        verify(meterRegistry).counter("analysis.packets.errors");
    }
}
