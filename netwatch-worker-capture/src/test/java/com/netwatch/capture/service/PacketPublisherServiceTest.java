package com.netwatch.capture.service;

import com.netwatch.capture.dto.PacketMessage;
import com.netwatch.capture.model.RawPacket;
import com.netwatch.capture.repository.RawPacketRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pcap4j.packet.Packet;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacketPublisherServiceTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock RawPacketRepository repository;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;

    @InjectMocks PacketPublisherService service;

    @BeforeEach
    void stubMetrics() {
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    // ── extractMetadata ──────────────────────────────────────────────────────

    @Test
    void extractMetadata_withEmptyPacket_returnsUnknownProtocol() {
        Packet packet = mock(Packet.class);
        when(packet.length()).thenReturn(64);
        when(packet.get(any())).thenReturn(null);

        PacketMessage msg = service.extractMetadata(packet);

        assertThat(msg.srcIp()).isEqualTo("unknown");
        assertThat(msg.dstIp()).isEqualTo("unknown");
        assertThat(msg.protocol()).isEqualTo("UNKNOWN");
        assertThat(msg.packetLength()).isEqualTo(64);
        assertThat(msg.srcPort()).isNull();
        assertThat(msg.dstPort()).isNull();
        assertThat(msg.capturedAt()).isNotNull();
    }

    // ── process ─────────────────────────────────────────────────────────────

    @Test
    void process_publishesMessageToRabbitMQ() {
        Packet packet = mock(Packet.class);
        when(packet.length()).thenReturn(100);
        when(packet.get(any())).thenReturn(null);

        service.process(packet);

        verify(rabbitTemplate).convertAndSend(
                eq("netwatch.direct"),
                eq("packets.raw"),
                any(PacketMessage.class));
    }

    @Test
    void process_persistsRawPacketToDatabase() {
        Packet packet = mock(Packet.class);
        when(packet.length()).thenReturn(100);
        when(packet.get(any())).thenReturn(null);

        service.process(packet);

        ArgumentCaptor<RawPacket> captor = ArgumentCaptor.forClass(RawPacket.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSrcIp()).isEqualTo("unknown");
        assertThat(captor.getValue().getProtocol()).isEqualTo("UNKNOWN");
    }

    @Test
    void process_incrementsProcessedCounter() {
        Packet packet = mock(Packet.class);
        when(packet.length()).thenReturn(50);
        when(packet.get(any())).thenReturn(null);

        service.process(packet);

        verify(counter).increment();
    }

    @Test
    void process_whenRabbitThrows_incrementsErrorCounter() {
        Packet packet = mock(Packet.class);
        when(packet.length()).thenReturn(50);
        when(packet.get(any())).thenReturn(null);
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        service.process(packet);   // no debe lanzar excepción

        verify(meterRegistry).counter("capture.packets.errors");
        verify(counter).increment();
    }
}
