package com.netwatch.analysis.service;

import com.netwatch.analysis.config.RabbitMQConfig;
import com.netwatch.analysis.dto.PacketMessage;
import com.netwatch.analysis.dto.ThreatMessage;
import com.netwatch.analysis.model.ThreatEvent;
import com.netwatch.analysis.repository.ThreatEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Consume paquetes de netwatch.packets.raw, aplica el motor STRIDE
 * y, si hay amenaza, la persiste y publica en threats.detected, alerts.notify
 * y osint.enrich.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PacketConsumerService {

    private final ThreatDetectionService detectionService;
    private final ThreatEventRepository  repository;
    private final RabbitTemplate         rabbitTemplate;
    private final MeterRegistry          meterRegistry;

    @RabbitListener(queues = RabbitMQConfig.PACKETS_RAW_QUEUE)
    public void onPacket(PacketMessage packet) {
        try {
            meterRegistry.counter("analysis.packets.received").increment();

            detectionService.analyze(packet).ifPresent(result -> {
                ThreatEvent event = repository.save(ThreatEvent.builder()
                        .srcIp(packet.srcIp())
                        .dstIp(packet.dstIp())
                        .srcPort(packet.srcPort())
                        .dstPort(packet.dstPort())
                        .protocol(packet.protocol())
                        .flags(packet.flags())
                        .threatType(result.threatType())
                        .severity(result.severity())
                        .description(result.description())
                        .build());

                ThreatMessage msg = toMessage(event);

                // Notificar al worker-alerts
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NETWATCH_EXCHANGE,
                        RabbitMQConfig.RK_ALERTS,
                        msg);

                // Enriquecer con OSINT
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NETWATCH_EXCHANGE,
                        RabbitMQConfig.RK_OSINT,
                        msg);

                meterRegistry.counter("analysis.threats.detected",
                        "type", result.threatType().name(),
                        "severity", result.severity().name()).increment();

                log.warn("Amenaza detectada: type={} severity={} src={}",
                        result.threatType(), result.severity(), packet.srcIp());
            });

        } catch (Exception e) {
            meterRegistry.counter("analysis.packets.errors").increment();
            log.error("Error analizando paquete: {}", e.getMessage(), e);
        }
    }

    private ThreatMessage toMessage(ThreatEvent event) {
        return new ThreatMessage(
                event.getId(),
                event.getSrcIp(),
                event.getDstIp(),
                event.getSrcPort(),
                event.getDstPort(),
                event.getProtocol(),
                event.getThreatType().name(),
                event.getSeverity().name(),
                event.getDescription(),
                event.getDetectedAt() != null ? event.getDetectedAt() : LocalDateTime.now()
        );
    }
}
