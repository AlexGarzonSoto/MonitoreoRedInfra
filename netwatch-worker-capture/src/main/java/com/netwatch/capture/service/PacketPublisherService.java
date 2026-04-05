package com.netwatch.capture.service;

import com.netwatch.capture.config.RabbitMQConfig;
import com.netwatch.capture.dto.PacketMessage;
import com.netwatch.capture.model.RawPacket;
import com.netwatch.capture.repository.RawPacketRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Extrae metadatos de cada paquete Pcap4J, los publica en RabbitMQ
 * (cola netwatch.packets.raw) y los persiste en PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PacketPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final RawPacketRepository repository;
    private final MeterRegistry meterRegistry;

    public void process(Packet packet) {
        try {
            PacketMessage msg = extractMetadata(packet);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NETWATCH_EXCHANGE,
                    RabbitMQConfig.RK_PACKETS,
                    msg);

            repository.save(RawPacket.builder()
                    .srcIp(msg.srcIp())
                    .dstIp(msg.dstIp())
                    .srcPort(msg.srcPort())
                    .dstPort(msg.dstPort())
                    .protocol(msg.protocol())
                    .flags(msg.flags())
                    .packetLength(msg.packetLength())
                    .build());

            meterRegistry.counter("capture.packets.processed",
                    "protocol", msg.protocol()).increment();

        } catch (Exception e) {
            log.error("Error procesando paquete: {}", e.getMessage());
            meterRegistry.counter("capture.packets.errors").increment();
        }
    }

    PacketMessage extractMetadata(Packet packet) {
        String srcIp    = "unknown";
        String dstIp    = "unknown";
        Integer srcPort = null;
        Integer dstPort = null;
        String protocol = "UNKNOWN";
        String flags    = null;
        int length      = packet.length();

        IpV4Packet ipV4 = packet.get(IpV4Packet.class);
        if (ipV4 != null) {
            srcIp    = ipV4.getHeader().getSrcAddr().getHostAddress();
            dstIp    = ipV4.getHeader().getDstAddr().getHostAddress();
            protocol = ipV4.getHeader().getProtocol().name();
        }

        TcpPacket tcp = packet.get(TcpPacket.class);
        if (tcp != null) {
            srcPort  = tcp.getHeader().getSrcPort().valueAsInt();
            dstPort  = tcp.getHeader().getDstPort().valueAsInt();
            protocol = "TCP";
            flags    = buildTcpFlags(tcp.getHeader());
        }

        UdpPacket udp = packet.get(UdpPacket.class);
        if (udp != null) {
            srcPort  = udp.getHeader().getSrcPort().valueAsInt();
            dstPort  = udp.getHeader().getDstPort().valueAsInt();
            protocol = "UDP";
        }

        return new PacketMessage(srcIp, dstIp, srcPort, dstPort,
                protocol, flags, length, LocalDateTime.now());
    }

    private String buildTcpFlags(TcpPacket.TcpHeader h) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(h.getSyn())) sb.append("SYN,");
        if (Boolean.TRUE.equals(h.getAck())) sb.append("ACK,");
        if (Boolean.TRUE.equals(h.getFin())) sb.append("FIN,");
        if (Boolean.TRUE.equals(h.getRst())) sb.append("RST,");
        if (Boolean.TRUE.equals(h.getPsh())) sb.append("PSH,");
        if (Boolean.TRUE.equals(h.getUrg())) sb.append("URG,");
        if (!sb.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
