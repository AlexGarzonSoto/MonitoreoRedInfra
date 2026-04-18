package com.netwatch.gateway.service;

import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.repository.AlertRepository;
import com.netwatch.gateway.repository.NetworkEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private NetworkEventRepository eventRepository;
    @Mock private AlertRepository alertRepository;

    @InjectMocks private ReportService reportService;

    private NetworkEvent buildEvent() {
        return NetworkEvent.builder()
                .id(UUID.randomUUID())
                .srcIp("10.0.0.1")
                .dstIp("192.168.1.1")
                .srcPort(54321)
                .dstPort(22)
                .protocol("TCP")
                .flags("SYN")
                .packetLength(64)
                .threatType(NetworkEvent.ThreatType.BRUTE_FORCE)
                .severity(NetworkEvent.Severity.HIGH)
                .description("Ataque de fuerza bruta al puerto 22")
                .country("Germany")
                .city("Frankfurt")
                .latitude(50.11)
                .longitude(8.68)
                .asn("AS9136")
                .abuseScore(85)
                .resolved(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private Alert buildAlert() {
        return Alert.builder()
                .id(UUID.randomUUID())
                .title("[HIGH] BRUTE_FORCE desde 10.0.0.1")
                .details("Múltiples intentos de login al puerto SSH")
                .status(Alert.AlertStatus.OPEN)
                .notificationSent(true)
                .eventId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── eventsAsJson ──────────────────────────────────────────────────────────

    @Test
    void eventsAsJson_retornaJsonConEventos() throws Exception {
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEvent())));

        byte[] json = reportService.eventsAsJson();

        String result = new String(json);
        assertThat(result).contains("\"total\"");
        assertThat(result).contains("\"events\"");
    }

    @Test
    void eventsAsJson_sinEventos_retornaJsonVacio() throws Exception {
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        byte[] json = reportService.eventsAsJson();

        String result = new String(json);
        assertThat(result).contains("\"total\" : 0");
    }

    // ── eventsAsCsv ───────────────────────────────────────────────────────────

    @Test
    void eventsAsCsv_conEventos_incluyeCabecera() {
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEvent())));

        byte[] csv = reportService.eventsAsCsv();

        String result = new String(csv);
        assertThat(result).startsWith("id,timestamp,srcIp");
        assertThat(result).contains("10.0.0.1");
        assertThat(result).contains("BRUTE_FORCE");
    }

    @Test
    void eventsAsCsv_sinEventos_soloCabecera() {
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        byte[] csv = reportService.eventsAsCsv();

        String result = new String(csv);
        assertThat(result).startsWith("id,timestamp,srcIp");
        // Solo la línea de cabecera + salto de línea
        assertThat(result.lines().count()).isEqualTo(1L);
    }

    @Test
    void eventsAsCsv_descripcionConComa_escapaCorrectamente() {
        NetworkEvent event = buildEvent();
        event.setDescription("texto, con coma");
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        byte[] csv = reportService.eventsAsCsv();

        assertThat(new String(csv)).contains("\"texto, con coma\"");
    }

    // ── eventsAsXml ───────────────────────────────────────────────────────────

    @Test
    void eventsAsXml_conEventos_generaXmlValido() {
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEvent())));

        byte[] xml = reportService.eventsAsXml();

        String result = new String(xml);
        assertThat(result).startsWith("<?xml");
        assertThat(result).contains("<report>");
        assertThat(result).contains("<event>");
        assertThat(result).contains("<srcIp>10.0.0.1</srcIp>");
    }

    @Test
    void eventsAsXml_descripcionConCaracteresEspeciales_escapaCorrectamente() {
        NetworkEvent event = buildEvent();
        event.setDescription("ataque <script> & \"xss\"");
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        byte[] xml = reportService.eventsAsXml();

        String result = new String(xml);
        assertThat(result).contains("&lt;script&gt;");
        assertThat(result).contains("&amp;");
        assertThat(result).contains("&quot;xss&quot;");
    }

    // ── alertsAsJson ──────────────────────────────────────────────────────────

    @Test
    void alertsAsJson_retornaJsonConAlertas() throws Exception {
        when(alertRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildAlert())));

        byte[] json = reportService.alertsAsJson();

        String result = new String(json);
        assertThat(result).contains("\"total\"");
        assertThat(result).contains("\"alerts\"");
    }

    // ── alertsAsCsv ───────────────────────────────────────────────────────────

    @Test
    void alertsAsCsv_conAlertas_incluyeCabecera() {
        when(alertRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildAlert())));

        byte[] csv = reportService.alertsAsCsv();

        String result = new String(csv);
        assertThat(result).startsWith("id,createdAt,title");
        assertThat(result).contains("OPEN");
    }

    // ── alertsAsXml ───────────────────────────────────────────────────────────

    @Test
    void alertsAsXml_conAlertas_generaXmlValido() {
        when(alertRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildAlert())));

        byte[] xml = reportService.alertsAsXml();

        String result = new String(xml);
        assertThat(result).contains("<type>alerts</type>");
        assertThat(result).contains("<alert>");
        assertThat(result).contains("<status>OPEN</status>");
    }
}
