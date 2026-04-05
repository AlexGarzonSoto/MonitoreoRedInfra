package com.netwatch.capture.dto;

import java.time.LocalDateTime;

/**
 * Mensaje publicado en netwatch.packets.raw para su análisis posterior.
 * Usa record de Java 21 (inmutable, serializable con Jackson).
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
