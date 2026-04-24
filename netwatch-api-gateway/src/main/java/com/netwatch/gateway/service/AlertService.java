package com.netwatch.gateway.service;

import com.netwatch.gateway.dto.AlertDTO;
import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.repository.AlertRepository;
import com.netwatch.gateway.repository.NetworkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private static final String MSG_NOT_FOUND = "Alerta no encontrada: ";

    private final AlertRepository alertRepository;
    private final NetworkEventRepository eventRepository;

    @Transactional(readOnly = true)
    public Page<AlertDTO> findAll(Alert.AlertStatus status, Pageable pageable) {
        Page<Alert> page = (status != null)
                ? alertRepository.findByStatus(status, pageable)
                : alertRepository.findAll(pageable);

        return page.map(alert -> {
            NetworkEvent event = alert.getEventId() != null
                    ? eventRepository.findById(alert.getEventId()).orElse(null)
                    : null;
            return AlertDTO.from(alert, event);
        });
    }

    @Transactional(readOnly = true)
    public AlertDTO findById(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(MSG_NOT_FOUND + id));
        NetworkEvent event = alert.getEventId() != null
                ? eventRepository.findById(alert.getEventId()).orElse(null)
                : null;
        return AlertDTO.from(alert, event);
    }

    @Transactional
    public AlertDTO acknowledge(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(MSG_NOT_FOUND + id));
        alert.setStatus(Alert.AlertStatus.ACKNOWLEDGED);
        alert = alertRepository.save(alert);
        log.info("Alerta reconocida: {}", id);
        return toDTO(alert);
    }

    @Transactional
    public AlertDTO resolve(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(MSG_NOT_FOUND + id));
        alert.setStatus(Alert.AlertStatus.RESOLVED);
        alert = alertRepository.save(alert);
        log.info("Alerta resuelta: {}", id);
        return toDTO(alert);
    }

    @Transactional
    public AlertDTO markFalsePositive(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(MSG_NOT_FOUND + id));
        alert.setStatus(Alert.AlertStatus.FALSE_POSITIVE);
        alert = alertRepository.save(alert);
        log.info("Alerta marcada como falso positivo: {}", id);
        return toDTO(alert);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getSummary() {
        return Map.of(
                "open",          alertRepository.countByStatus(Alert.AlertStatus.OPEN),
                "acknowledged",  alertRepository.countByStatus(Alert.AlertStatus.ACKNOWLEDGED),
                "resolved",      alertRepository.countByStatus(Alert.AlertStatus.RESOLVED),
                "falsePositive", alertRepository.countByStatus(Alert.AlertStatus.FALSE_POSITIVE)
        );
    }

    private AlertDTO toDTO(Alert alert) {
        NetworkEvent event = alert.getEventId() != null
                ? eventRepository.findById(alert.getEventId()).orElse(null)
                : null;
        return AlertDTO.from(alert, event);
    }
}
