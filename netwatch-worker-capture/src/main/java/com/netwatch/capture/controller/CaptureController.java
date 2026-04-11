package com.netwatch.capture.controller;

import com.netwatch.capture.config.CaptureProperties;
import com.netwatch.capture.service.PacketCaptureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.NetworkInterface;
import java.util.*;

/**
 * REST API interna del worker-capture para gestión de interfaces de red.
 * Accesible desde la red interna de Docker (netwatch-net) en el puerto 8082.
 *
 * GET  /capture/interfaces   → lista interfaces disponibles
 * GET  /capture/status       → estado actual de captura
 * PATCH /capture/interface   → cambiar interfaz activa
 */
@RestController
@RequestMapping("/capture")
@RequiredArgsConstructor
@Slf4j
public class CaptureController {

    private final PacketCaptureService captureService;
    private final CaptureProperties captureProperties;

    /**
     * Lista todas las interfaces de red disponibles en el sistema.
     * Intenta usar Pcap4J primero; si no está disponible, usa Java stdlib.
     */
    @GetMapping("/interfaces")
    public ResponseEntity<Map<String, Object>> listInterfaces() {
        List<Map<String, String>> interfaces = new ArrayList<>();

        // Intentar con Pcap4J
        try {
            List<PcapNetworkInterface> pcapIfs = Pcaps.findAllDevs();
            if (pcapIfs != null) {
                for (PcapNetworkInterface nif : pcapIfs) {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("name", nif.getName());
                    entry.put("description", nif.getDescription() != null ? nif.getDescription() : "");
                    entry.put("source", "pcap4j");
                    interfaces.add(entry);
                }
            }
        } catch (Exception e) {
            log.debug("Pcap4J no disponible para listar interfaces: {}", e.getMessage());
        }

        // Fallback: Java stdlib
        if (interfaces.isEmpty()) {
            try {
                Enumeration<NetworkInterface> javaIfs = NetworkInterface.getNetworkInterfaces();
                if (javaIfs != null) {
                    for (NetworkInterface nif : Collections.list(javaIfs)) {
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("name", nif.getName());
                        entry.put("description", nif.getDisplayName());
                        entry.put("source", "java");
                        interfaces.add(entry);
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudieron listar interfaces de red: {}", e.getMessage());
            }
        }

        // Si aún vacío, devolver interfaz simulada
        if (interfaces.isEmpty()) {
            interfaces.add(Map.of("name", "eth0", "description", "Interfaz simulada", "source", "simulation"));
            interfaces.add(Map.of("name", "lo",   "description", "Loopback",          "source", "simulation"));
        }

        return ResponseEntity.ok(Map.of(
            "interfaces",        interfaces,
            "current",           captureProperties.getNetworkInterface(),
            "captureRunning",    captureService.isRunning(),
            "promiscuousMode",   captureProperties.isPromiscuous()
        ));
    }

    /**
     * Retorna el estado actual de la captura.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
            "running",         captureService.isRunning(),
            "interface",       captureProperties.getNetworkInterface(),
            "promiscuous",     captureProperties.isPromiscuous(),
            "enabled",         captureProperties.isEnabled(),
            "snapLen",         captureProperties.getSnapLen()
        ));
    }

    /**
     * Cambia la interfaz de red activa y reinicia la captura.
     * Body: { "interface": "eth0" }
     */
    @PatchMapping("/interface")
    public ResponseEntity<Map<String, Object>> changeInterface(
            @RequestBody Map<String, String> body) {

        String newInterface = body.get("interface");
        if (newInterface == null || newInterface.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "El campo 'interface' es obligatorio"));
        }

        String previous = captureProperties.getNetworkInterface();
        log.info("Cambiando interfaz de captura: {} → {}", previous, newInterface);

        captureService.changeInterface(newInterface);

        return ResponseEntity.ok(Map.of(
            "previous",  previous,
            "current",   newInterface,
            "running",   captureService.isRunning(),
            "message",   "Interfaz cambiada. Captura reiniciada en: " + newInterface
        ));
    }

    /**
     * Detiene la captura sin cambiar la interfaz.
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        captureService.stopCapture();
        return ResponseEntity.ok(Map.of("running", false, "message", "Captura detenida"));
    }

    /**
     * Inicia (o reinicia) la captura con la interfaz actual.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        captureService.startCapture();
        return ResponseEntity.ok(Map.of(
            "running",   captureService.isRunning(),
            "interface", captureProperties.getNetworkInterface(),
            "message",   "Captura iniciada en: " + captureProperties.getNetworkInterface()
        ));
    }
}
