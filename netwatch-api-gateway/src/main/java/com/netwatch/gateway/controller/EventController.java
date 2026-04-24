package com.netwatch.gateway.controller;

import com.netwatch.gateway.model.NetworkEvent;
import com.netwatch.gateway.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Page<NetworkEvent>> getEvents(
            @RequestParam(required = false) NetworkEvent.Severity severity,
            @RequestParam(required = false) NetworkEvent.ThreatType threatType,
            @RequestParam(required = false) String srcIp,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {

        return ResponseEntity.ok(
            eventService.findEvents(severity, threatType, srcIp, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'VIEWER')")
    public ResponseEntity<NetworkEvent> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.findById(id));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<NetworkEvent> resolveEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.resolve(id));
    }

    @GetMapping("/stats/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'VIEWER')")
    public ResponseEntity<java.util.Map<String, Object>> getSummary() {
        return ResponseEntity.ok(eventService.getSummary());
    }
}