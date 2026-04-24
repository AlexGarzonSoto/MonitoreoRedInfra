package com.netwatch.analysis.service;

import com.netwatch.analysis.dto.PacketMessage;
import com.netwatch.analysis.model.ThreatEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Motor de detección de amenazas basado en el modelo STRIDE.
 *
 * Implementa reglas estáticas sobre características del paquete.
 * En producción se complementaría con análisis de series temporales
 * (p.ej. Apache Flink) para detección de patrones multi-paquete.
 *
 * Clasificación STRIDE aplicada:
 *  - Spoofing        → no detectado a nivel L4 (requiere L2/ARP)
 *  - Tampering       → DATA_EXFILTRATION (modificación/robo de datos)
 *  - Repudiation     → no aplica en este nivel
 *  - Information Disclosure → PORT_SCAN, DNS_TUNNELING
 *  - Denial of Service → SYN_FLOOD
 *  - Elevation of Privilege → BRUTE_FORCE, MALWARE_C2
 */
@Service
@Slf4j
public class ThreatDetectionService {

    private static final String DESDE = " desde ";

    // Puertos de administración comúnmente atacados (BRUTE_FORCE)
    private static final java.util.Set<Integer> ADMIN_PORTS =
            java.util.Set.of(22, 23, 3389, 5900, 21, 5985);

    // Puertos frecuentes en Command & Control de malware
    private static final java.util.Set<Integer> C2_PORTS =
            java.util.Set.of(4444, 1337, 8888, 9999, 6666, 31337, 12345);

    /**
     * Analiza un paquete y retorna la amenaza detectada, o vacío si es tráfico normal.
     */
    public Optional<DetectionResult> analyze(PacketMessage packet) {

        // 1. SYN_FLOOD — DoS: muchos SYN sin ACK (flags solo SYN)
        if ("SYN".equals(packet.flags())) {
            return Optional.of(new DetectionResult(
                    ThreatEvent.ThreatType.SYN_FLOOD,
                    ThreatEvent.Severity.HIGH,
                    "SYN flood detectado: paquete TCP con flag SYN sin ACK desde "
                            + packet.srcIp()));
        }

        // 2. BRUTE_FORCE — Elevación de privilegios: ataques a puertos admin
        if ("TCP".equals(packet.protocol())
                && packet.dstPort() != null
                && ADMIN_PORTS.contains(packet.dstPort())) {
            return Optional.of(new DetectionResult(
                    ThreatEvent.ThreatType.BRUTE_FORCE,
                    ThreatEvent.Severity.MEDIUM,
                    "Posible brute-force al puerto " + packet.dstPort()
                            + DESDE + packet.srcIp()));
        }

        // 3. MALWARE_C2 — Elevación de privilegios: comunicación con C&C
        if (packet.dstPort() != null && C2_PORTS.contains(packet.dstPort())) {
            return Optional.of(new DetectionResult(
                    ThreatEvent.ThreatType.MALWARE_C2,
                    ThreatEvent.Severity.CRITICAL,
                    "Posible comunicación C2 al puerto " + packet.dstPort()
                            + DESDE + packet.srcIp()));
        }

        // 4. DNS_TUNNELING — Divulgación de información: DNS con payload grande
        if ("UDP".equals(packet.protocol())
                && packet.dstPort() != null
                && packet.dstPort() == 53
                && packet.packetLength() != null
                && packet.packetLength() > 512) {
            return Optional.of(new DetectionResult(
                    ThreatEvent.ThreatType.DNS_TUNNELING,
                    ThreatEvent.Severity.HIGH,
                    "Posible DNS tunneling: paquete UDP/53 de " + packet.packetLength()
                            + " bytes desde " + packet.srcIp()));
        }

        // 5. DATA_EXFILTRATION — Alteración: tráfico saliente grande en puertos no estándar
        if (packet.dstPort() != null
                && packet.dstPort() > 1024
                && packet.packetLength() != null
                && packet.packetLength() > 8192) {
            return Optional.of(new DetectionResult(
                    ThreatEvent.ThreatType.DATA_EXFILTRATION,
                    ThreatEvent.Severity.HIGH,
                    "Posible exfiltración: paquete de " + packet.packetLength()
                            + " bytes al puerto " + packet.dstPort()
                            + DESDE + packet.srcIp()));
        }

        // 6. PORT_SCAN — Divulgación: escaneo a puertos bajos (<1024)
        if (packet.dstPort() != null && packet.dstPort() < 1024
                && packet.srcPort() != null && packet.srcPort() > 1024) {
            return Optional.of(new DetectionResult(
                    ThreatEvent.ThreatType.PORT_SCAN,
                    ThreatEvent.Severity.MEDIUM,
                    "Posible escaneo de puertos: acceso al puerto " + packet.dstPort()
                            + DESDE + packet.srcIp() + ":" + packet.srcPort()));
        }

        return Optional.empty();
    }

    public record DetectionResult(
            ThreatEvent.ThreatType threatType,
            ThreatEvent.Severity severity,
            String description
    ) {}
}
