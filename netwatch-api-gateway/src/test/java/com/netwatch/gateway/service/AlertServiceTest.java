package com.netwatch.gateway.service;

import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @InjectMocks private AlertService alertService;

    private Alert openAlert;
    private final UUID alertId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        openAlert = Alert.builder()
                .id(alertId)
                .title("[HIGH] PORT_SCAN desde 10.0.0.1")
                .details("Escaneo de puertos detectado")
                .status(Alert.AlertStatus.OPEN)
                .notificationSent(false)
                .build();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_conFiltroStatus_usaFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Alert> page = new PageImpl<>(List.of(openAlert));
        when(alertRepository.findByStatus(Alert.AlertStatus.OPEN, pageable)).thenReturn(page);

        Page<Alert> result = alertService.findAll(Alert.AlertStatus.OPEN, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(alertRepository).findByStatus(Alert.AlertStatus.OPEN, pageable);
    }

    @Test
    void findAll_sinFiltro_usaFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Alert> page = new PageImpl<>(List.of(openAlert));
        when(alertRepository.findAll(pageable)).thenReturn(page);

        Page<Alert> result = alertService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(alertRepository).findAll(pageable);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_conIdExistente_retornaAlerta() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert));

        Alert result = alertService.findById(alertId);

        assertThat(result).isEqualTo(openAlert);
    }

    @Test
    void findById_conIdInexistente_lanzaNoSuchElement() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.findById(alertId))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── acknowledge ───────────────────────────────────────────────────────────

    @Test
    void acknowledge_cambiaEstadoAAcknowledged() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert));
        when(alertRepository.save(openAlert)).thenReturn(openAlert);

        Alert result = alertService.acknowledge(alertId);

        assertThat(result.getStatus()).isEqualTo(Alert.AlertStatus.ACKNOWLEDGED);
        verify(alertRepository).save(openAlert);
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Test
    void resolve_cambiaEstadoAResolved() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert));
        when(alertRepository.save(openAlert)).thenReturn(openAlert);

        Alert result = alertService.resolve(alertId);

        assertThat(result.getStatus()).isEqualTo(Alert.AlertStatus.RESOLVED);
        verify(alertRepository).save(openAlert);
    }

    // ── markFalsePositive ─────────────────────────────────────────────────────

    @Test
    void markFalsePositive_cambiaEstadoAFalsePositive() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert));
        when(alertRepository.save(openAlert)).thenReturn(openAlert);

        Alert result = alertService.markFalsePositive(alertId);

        assertThat(result.getStatus()).isEqualTo(Alert.AlertStatus.FALSE_POSITIVE);
        verify(alertRepository).save(openAlert);
    }

    // ── getSummary ────────────────────────────────────────────────────────────

    @Test
    void getSummary_retornaContadoresPorEstado() {
        when(alertRepository.countByStatus(Alert.AlertStatus.OPEN)).thenReturn(10L);
        when(alertRepository.countByStatus(Alert.AlertStatus.ACKNOWLEDGED)).thenReturn(3L);
        when(alertRepository.countByStatus(Alert.AlertStatus.RESOLVED)).thenReturn(7L);
        when(alertRepository.countByStatus(Alert.AlertStatus.FALSE_POSITIVE)).thenReturn(2L);

        Map<String, Long> summary = alertService.getSummary();

        assertThat(summary.get("open")).isEqualTo(10L);
        assertThat(summary.get("acknowledged")).isEqualTo(3L);
        assertThat(summary.get("resolved")).isEqualTo(7L);
        assertThat(summary.get("falsePositive")).isEqualTo(2L);
    }
}
