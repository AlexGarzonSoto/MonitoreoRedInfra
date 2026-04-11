package com.netwatch.scanner.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanResult {

    private String scanId;
    private String targetIp;

    /**
     * Puertos abiertos detectados por Nmap.
     * Formato: "22/tcp open ssh OpenSSH 8.2p1"
     */
    private List<String> openPorts;

    /** Vulnerabilidades correlacionadas con el NVD para cada servicio detectado. */
    private List<Vulnerability> vulnerabilities;

    /** COMPLETED | ERROR */
    private String status;

    /** Mensaje de error si status = ERROR. */
    private String errorMessage;

    private LocalDateTime scannedAt;
}
