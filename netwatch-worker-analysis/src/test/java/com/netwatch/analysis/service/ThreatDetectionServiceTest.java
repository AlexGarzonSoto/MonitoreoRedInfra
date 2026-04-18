package com.netwatch.analysis.service;

import com.netwatch.analysis.dto.PacketMessage;
import com.netwatch.analysis.model.ThreatEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ThreatDetectionServiceTest {

    private final ThreatDetectionService service = new ThreatDetectionService();

    private PacketMessage packet(String protocol, Integer srcPort, Integer dstPort,
                                 String flags, Integer length) {
        return new PacketMessage("1.2.3.4", "5.6.7.8",
                srcPort, dstPort, protocol, flags, length, LocalDateTime.now());
    }

    @Test
    void analyze_synFlood_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 54321, 80, "SYN", 60));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.SYN_FLOOD);
        assertThat(result.get().severity()).isEqualTo(ThreatEvent.Severity.HIGH);
    }

    @Test
    void analyze_bruteForce_ssh_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 55000, 22, "SYN,ACK", 60));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.BRUTE_FORCE);
        assertThat(result.get().severity()).isEqualTo(ThreatEvent.Severity.MEDIUM);
    }

    @Test
    void analyze_bruteForce_rdp_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 55000, 3389, "SYN,ACK", 60));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.BRUTE_FORCE);
    }

    @Test
    void analyze_malwareC2_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 55000, 4444, null, 200));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.MALWARE_C2);
        assertThat(result.get().severity()).isEqualTo(ThreatEvent.Severity.CRITICAL);
    }

    @Test
    void analyze_dnsTunneling_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("UDP", 12345, 53, null, 1024));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.DNS_TUNNELING);
        assertThat(result.get().severity()).isEqualTo(ThreatEvent.Severity.HIGH);
    }

    @Test
    void analyze_dnsTunneling_smallPacket_notDetected() {
        // Paquete DNS normal (< 512 bytes) no debe activar la regla de tunneling.
        // srcPort=53 (respuesta DNS servidor→servidor) para evitar que la regla
        // PORT_SCAN (srcPort>1024 && dstPort<1024) se active falsamente.
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("UDP", 53, 53, null, 100));

        assertThat(result).isEmpty();
    }

    @Test
    void analyze_dataExfiltration_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 55000, 9090, "ACK", 10000));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.DATA_EXFILTRATION);
    }

    @Test
    void analyze_portScan_detected() {
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 55000, 443, "SYN,ACK", 60));

        assertThat(result).isPresent();
        assertThat(result.get().threatType()).isEqualTo(ThreatEvent.ThreatType.PORT_SCAN);
    }

    @Test
    void analyze_normalTraffic_returnsEmpty() {
        // Tráfico HTTP normal: puerto alto → 8080, paquete pequeño, sin flags SYN solo
        Optional<ThreatDetectionService.DetectionResult> result =
                service.analyze(packet("TCP", 55000, 8080, "ACK", 500));

        assertThat(result).isEmpty();
    }
}
