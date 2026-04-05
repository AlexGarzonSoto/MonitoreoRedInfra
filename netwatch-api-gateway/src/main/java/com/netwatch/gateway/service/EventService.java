package com.netwatch.gateway.service;

import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.repository.NetworkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final NetworkEventRepository eventRepository;

    @Transactional(readOnly = true)
    public Page<NetworkEvent> findEvents(
            NetworkEvent.Severity severity,
            NetworkEvent.ThreatType threatType,
            String srcIp,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        return eventRepository.findByFilters(severity, threatType, srcIp, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public NetworkEvent findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evento no encontrado: " + id));
    }

    @Transactional
    public NetworkEvent resolve(UUID id) {
        NetworkEvent event = findById(id);
        event.setResolved(true);
        log.info("Evento resuelto: {}", id);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        return Map.of(
                "total",     eventRepository.count(),
                "unresolved", eventRepository.countUnresolved(),
                "critical",  eventRepository.countBySeverity(NetworkEvent.Severity.CRITICAL),
                "high",      eventRepository.countBySeverity(NetworkEvent.Severity.HIGH),
                "medium",    eventRepository.countBySeverity(NetworkEvent.Severity.MEDIUM)
        );
    }
}
