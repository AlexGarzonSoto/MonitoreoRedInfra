package com.netwatch.scanner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta Nmap mediante ProcessBuilder para detectar puertos abiertos
 * y versiones de servicios en un host objetivo.
 *
 * Nmap debe estar instalado en la imagen Docker (apk add nmap nmap-scripts).
 */
@Service
@Slf4j
public class NmapScannerService {

    @Value("${netwatch.scanner.nmap-timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${netwatch.scanner.dry-run:false}")
    private boolean dryRun;

    /**
     * Escanea el host objetivo y devuelve las líneas de puertos abiertos.
     *
     * @param targetIp    IP o CIDR a escanear
     * @param targetPorts lista de puertos específicos; null = top-1000 de Nmap
     * @return líneas con formato "22/tcp open ssh OpenSSH 8.2p1 Ubuntu 4ubuntu0.3"
     */
    public List<String> scan(String targetIp, List<Integer> targetPorts) {
        if (dryRun) {
            log.info("[DRY-RUN] Simulando escaneo Nmap de {}", targetIp);
            return simulatedPorts();
        }

        List<String> cmd = buildNmapCommand(targetIp, targetPorts);
        log.info("Ejecutando: {}", String.join(" ", cmd));

        List<String> openPorts = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("nmap: {}", line);
                    // Las líneas de puertos abiertos tienen el formato:
                    // "22/tcp   open  ssh     OpenSSH 8.2p1"
                    if ((line.contains("/tcp") || line.contains("/udp")) && line.contains("open")) {
                        openPorts.add(line.trim());
                    }
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Escaneo de {} superó el timeout de {}s", targetIp, timeoutSeconds);
            }

            log.info("Escaneo de {} completado — {} puertos abiertos", targetIp, openPorts.size());
            return openPorts;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Escaneo de {} interrumpido", targetIp);
            return openPorts;
        } catch (Exception e) {
            log.error("Error ejecutando Nmap para {}: {}", targetIp, e.getMessage());
            throw new RuntimeException("Nmap falló: " + e.getMessage(), e);
        }
    }

    private List<String> buildNmapCommand(String targetIp, List<Integer> targetPorts) {
        List<String> cmd = new ArrayList<>();
        cmd.add("nmap");
        cmd.add("-sV");          // detección de versión de servicios
        cmd.add("--open");       // solo puertos abiertos
        cmd.add("-T4");          // velocidad agresiva
        cmd.add("--host-timeout");
        cmd.add(timeoutSeconds + "s");

        if (targetPorts != null && !targetPorts.isEmpty()) {
            StringBuilder ports = new StringBuilder("-p");
            for (int i = 0; i < targetPorts.size(); i++) {
                if (i > 0) ports.append(",");
                ports.append(targetPorts.get(i));
            }
            cmd.add(ports.toString());
        }

        cmd.add(targetIp);
        return cmd;
    }

    /**
     * Simulación para entornos sin red o pruebas de integración.
     * Devuelve puertos típicos de un servidor Linux expuesto.
     */
    private List<String> simulatedPorts() {
        return List.of(
            "22/tcp   open  ssh      OpenSSH 7.4p1 Debian",
            "80/tcp   open  http     Apache httpd 2.4.41",
            "443/tcp  open  ssl/http Apache httpd 2.4.41",
            "5432/tcp open  postgresql PostgreSQL DB 12.8"
        );
    }
}
