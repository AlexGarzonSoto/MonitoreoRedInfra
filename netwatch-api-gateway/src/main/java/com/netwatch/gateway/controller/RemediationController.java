package com.netwatch.gateway.controller;

import com.netwatch.gateway.service.RemediationService;
import com.netwatch.gateway.service.RemediationService.RemediationInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Proporciona información de remediación para tipos de amenazas.
 *
 * GET /api/v1/remediation             → todos los tipos
 * GET /api/v1/remediation/{threatType} → remediación específica
 * GET /api/v1/remediation/{threatType}/cves → CVEs en NVD (requiere conectividad)
 */
@RestController
@RequestMapping("/api/v1/remediation")
@RequiredArgsConstructor
public class RemediationController {

    private final RemediationService remediationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<List<RemediationInfo>> getAll() {
        return ResponseEntity.ok(remediationService.getAllRemediations());
    }

    @GetMapping("/{threatType}")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<RemediationInfo> getByType(@PathVariable String threatType) {
        return ResponseEntity.ok(remediationService.getRemediation(threatType));
    }

    @GetMapping("/{threatType}/cves")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCves(@PathVariable String threatType) {
        List<Map<String, String>> cves = remediationService.queryCves(threatType);
        RemediationInfo info = remediationService.getRemediation(threatType);
        return ResponseEntity.ok(Map.of(
            "threatType", threatType,
            "mitreTechnique", info.mitreTechnique(),
            "liveCves", cves,
            "localCves", info.relatedCves()
        ));
    }
}
