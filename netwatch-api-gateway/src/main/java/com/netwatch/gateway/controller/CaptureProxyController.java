package com.netwatch.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxy que expone la API de gestión de interfaces de red del worker-capture.
 * Traduce peticiones públicas (autenticadas) al servicio interno en puerto 8082.
 *
 * GET   /api/v1/capture/interfaces  → lista interfaces disponibles
 * GET   /api/v1/capture/status      → estado actual
 * PATCH /api/v1/capture/interface   → cambiar interfaz (solo ADMIN)
 * POST  /api/v1/capture/start       → iniciar captura  (solo ADMIN)
 * POST  /api/v1/capture/stop        → detener captura  (solo ADMIN)
 */
@RestController
@RequestMapping("/api/v1/capture")
@RequiredArgsConstructor
@Slf4j
public class CaptureProxyController {

    private static final String WORKER_UNAVAILABLE = "Worker-capture no disponible: ";
    private static final String KEY_ERROR = "error";

    private final RestTemplate restTemplate;

    @Value("${netwatch.capture.worker.url:http://worker-capture:8082}")
    private String workerCaptureUrl;

    @GetMapping("/interfaces")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Object> listInterfaces() {
        return proxyGet("/capture/interfaces");
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Object> status() {
        return proxyGet("/capture/status");
    }

    @PatchMapping("/interface")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> changeInterface(@RequestBody Map<String, String> body) {
        try {
            Object result = restTemplate.patchForObject(
                workerCaptureUrl + "/capture/interface", body, Object.class);
            return ResponseEntity.ok(result);
        } catch (RestClientException e) {
            log.warn("Error contactando worker-capture para cambio de interfaz: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Map.of("error", WORKER_UNAVAILABLE + e.getMessage()));
        }
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> start() {
        try {
            Object result = restTemplate.postForObject(
                workerCaptureUrl + "/capture/start", null, Object.class);
            return ResponseEntity.ok(result);
        } catch (RestClientException e) {
            log.warn("Error iniciando captura: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Map.of("error", WORKER_UNAVAILABLE + e.getMessage()));
        }
    }

    @PostMapping("/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> stop() {
        try {
            restTemplate.postForObject(workerCaptureUrl + "/capture/stop", null, Object.class);
            return ResponseEntity.ok(Map.of("message", "Captura detenida"));
        } catch (RestClientException e) {
            log.warn("Error deteniendo captura: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Map.of("error", WORKER_UNAVAILABLE + e.getMessage()));
        }
    }

    private ResponseEntity<Object> proxyGet(String path) {
        try {
            Object result = restTemplate.getForObject(workerCaptureUrl + path, Object.class);
            return ResponseEntity.ok(result);
        } catch (RestClientException e) {
            log.warn("Worker-capture no accesible en {}: {}", workerCaptureUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "interfaces",     java.util.List.of(
                    Map.of("name", "eth0", "description", "Por defecto", "source", "fallback"),
                    Map.of("name", "lo",   "description", "Loopback",    "source", "fallback")
                ),
                "current",        "eth0",
                "captureRunning", false,
                "workerStatus",   "no disponible"
            ));
        }
    }
}
