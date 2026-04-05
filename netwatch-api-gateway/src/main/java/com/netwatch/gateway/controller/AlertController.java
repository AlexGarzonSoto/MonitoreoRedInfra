package com.netwatch.gateway.controller;

import com.netwatch.gateway.model.Alert;
import com.netwatch.gateway.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Page<Alert>> getAlerts(
            @RequestParam(required = false) Alert.AlertStatus status,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(alertService.findAll(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Alert> getAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.findById(id));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Alert> acknowledge(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.acknowledge(id));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Alert> resolve(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.resolve(id));
    }

    @PatchMapping("/{id}/false-positive")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Alert> markFalsePositive(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.markFalsePositive(id));
    }

    @GetMapping("/stats/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Map<String, Long>> getSummary() {
        return ResponseEntity.ok(alertService.getSummary());
    }
}
