package com.netwatch.gateway.service;

import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.repository.AlertRepository;
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

    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public Page<Alert> findAll(Alert.AlertStatus status, Pageable pageable) {
        if (status != null) {
            return alertRepository.findByStatus(status, pageable);
        }
        return alertRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Alert findById(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Alerta no encontrada: " + id));
    }

    @Transactional
    public Alert acknowledge(UUID id) {
        Alert alert = findById(id);
        alert.setStatus(Alert.AlertStatus.ACKNOWLEDGED);
        log.info("Alerta reconocida: {}", id);
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert resolve(UUID id) {
        Alert alert = findById(id);
        alert.setStatus(Alert.AlertStatus.RESOLVED);
        log.info("Alerta resuelta: {}", id);
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert markFalsePositive(UUID id) {
        Alert alert = findById(id);
        alert.setStatus(Alert.AlertStatus.FALSE_POSITIVE);
        log.info("Alerta marcada como falso positivo: {}", id);
        return alertRepository.save(alert);
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
}
