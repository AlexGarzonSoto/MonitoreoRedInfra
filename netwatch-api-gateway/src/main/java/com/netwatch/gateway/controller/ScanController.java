package com.netwatch.gateway.controller;

import com.netwatch.gateway.model.ScanRequestMessage;
import com.netwatch.gateway.model.ScanResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Expone los endpoints REST para solicitar escaneos de vulnerabilidades
 * y consultar sus resultados.
 *
 * POST  /api/v1/scan/request           → encola el escaneo (ADMIN)
 * GET   /api/v1/scan/results           → lista todos los resultados (ANALYST, ADMIN)
 * GET   /api/v1/scan/results/{scanId}  → resultado de un escaneo concreto
 */
@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private static final String EXCHANGE       = "netwatch.direct";
    private static final String RK_SCAN_REQUEST = "scan.request";

    private final RabbitTemplate rabbitTemplate;

    /**
     * Almacén en memoria de resultados de escaneo.
     * Clave: scanId  Valor: ScanResult recibido desde worker-scanner.
     * En producción se reemplazaría por Valkey con TTL.
     */
    private final Map<String, ScanResult> resultStore = new ConcurrentHashMap<>();

    // ── Solicitar escaneo ─────────────────────────────────────────────────────

    @PostMapping("/request")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> requestScan(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        String targetIp = (String) body.get("targetIp");
        if (targetIp == null || targetIp.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El campo 'targetIp' es obligatorio"));
        }

        @SuppressWarnings("unchecked")
        java.util.List<Integer> ports = (java.util.List<Integer>) body.get("targetPorts");

        String scanId = UUID.randomUUID().toString();
        ScanRequestMessage request = ScanRequestMessage.builder()
                .scanId(scanId)
                .targetIp(targetIp)
                .targetPorts(ports)
                .requestedBy(auth.getName())
                .requestedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, RK_SCAN_REQUEST, request);

        log.info("Escaneo encolado — id={} target={} por={}", scanId, targetIp, auth.getName());
        return ResponseEntity.accepted()
                .body(Map.of("scanId", scanId, "status", "QUEUED", "targetIp", targetIp));
    }

    // ── Consultar resultados ──────────────────────────────────────────────────

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Collection<ScanResult>> listResults() {
        return ResponseEntity.ok(resultStore.values());
    }

    @GetMapping("/results/{scanId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ScanResult> getResult(@PathVariable String scanId) {
        ScanResult result = resultStore.get(scanId);
        if (result == null) {
            throw new NoSuchElementException("Escaneo no encontrado o pendiente: " + scanId);
        }
        return ResponseEntity.ok(result);
    }

    // ── Listener RabbitMQ: recibe resultados del worker-scanner ───────────────

    @RabbitListener(queues = "netwatch.scan.results")
    public void recibirResultado(ScanResult result) {
        log.info("Resultado recibido — id={} status={} puertos={}",
            result.getScanId(), result.getStatus(),
            result.getOpenPorts() != null ? result.getOpenPorts().size() : 0);
        resultStore.put(result.getScanId(), result);
    }
}
