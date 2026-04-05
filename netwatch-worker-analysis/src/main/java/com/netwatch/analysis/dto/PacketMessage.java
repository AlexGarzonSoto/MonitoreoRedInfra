package com.netwatch.analysis.dto;

import java.time.LocalDateTime;

/**
 * Mensaje recibido desde la cola netwatch.packets.raw.
 * Debe tener la misma estructura que el PacketMessage del worker-capture.
 */
public record PacketMessage(
        String srcIp,
        String dstIp,
        Integer srcPort,
        Integer dstPort,
        String protocol,
        String flags,
        Integer packetLength,
        LocalDateTime capturedAt
) {}
