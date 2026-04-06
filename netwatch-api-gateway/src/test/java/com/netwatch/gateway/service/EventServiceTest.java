package com.netwatch.gateway.service;

import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.repository.NetworkEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private NetworkEventRepository eventRepository;
    @InjectMocks private EventService eventService;

    private NetworkEvent buildEvent(NetworkEvent.Severity severity) {
        return NetworkEvent.builder()
                .id(UUID.randomUUID())
                .srcIp("192.168.1.10")
                .dstIp("10.0.0.1")
                .protocol("TCP")
                .threatType(NetworkEvent.ThreatType.PORT_SCAN)
                .severity(severity)
                .resolved(false)
                .build();
    }

    // ── findEvents ────────────────────────────────────────────────────────────

    @Test
    void findEvents_sinFiltros_usaFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<NetworkEvent> page = new PageImpl<>(List.of(buildEvent(NetworkEvent.Severity.HIGH)));
        when(eventRepository.findAll(pageable)).thenReturn(page);

        Page<NetworkEvent> result = eventService.findEvents(null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findAll(pageable);
    }

    @Test
    void findEvents_conFiltroSeverity_usaFindByFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<NetworkEvent> page = new PageImpl<>(List.of(buildEvent(NetworkEvent.Severity.CRITICAL)));
        when(eventRepository.findByFilters(eq("CRITICAL"), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<NetworkEvent> result = eventService.findEvents(
                NetworkEvent.Severity.CRITICAL, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findEvents_conFiltroSrcIp_usaFindByFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<NetworkEvent> page = new PageImpl<>(List.of(buildEvent(NetworkEvent.Severity.MEDIUM)));
        when(eventRepository.findByFilters(isNull(), isNull(), eq("10.0.0.5"), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<NetworkEvent> result = eventService.findEvents(
                null, null, "10.0.0.5", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_conIdExistente_retornaEvento() {
        UUID id = UUID.randomUUID();
        NetworkEvent event = buildEvent(NetworkEvent.Severity.HIGH);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        NetworkEvent result = eventService.findById(id);

        assertThat(result).isEqualTo(event);
    }

    @Test
    void findById_conIdInexistente_lanzaNoSuchElement() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Test
    void resolve_marcaEventoComoResuelto() {
        UUID id = UUID.randomUUID();
        NetworkEvent event = buildEvent(NetworkEvent.Severity.HIGH);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        NetworkEvent result = eventService.resolve(id);

        assertThat(result.isResolved()).isTrue();
        verify(eventRepository).save(event);
    }

    // ── getSummary ────────────────────────────────────────────────────────────

    @Test
    void getSummary_retornaContadoresCorrectos() {
        when(eventRepository.count()).thenReturn(100L);
        when(eventRepository.countUnresolved()).thenReturn(25L);
        when(eventRepository.countBySeverity(NetworkEvent.Severity.CRITICAL)).thenReturn(5L);
        when(eventRepository.countBySeverity(NetworkEvent.Severity.HIGH)).thenReturn(10L);
        when(eventRepository.countBySeverity(NetworkEvent.Severity.MEDIUM)).thenReturn(15L);

        var summary = eventService.getSummary();

        assertThat(summary.get("total")).isEqualTo(100L);
        assertThat(summary.get("unresolved")).isEqualTo(25L);
        assertThat(summary.get("critical")).isEqualTo(5L);
        assertThat(summary.get("high")).isEqualTo(10L);
        assertThat(summary.get("medium")).isEqualTo(15L);
    }
}
