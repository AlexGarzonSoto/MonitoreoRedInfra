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
public class ScanRequestMessage {

    /** Identificador único del escaneo (UUID generado por el api-gateway). */
    private String scanId;

    /** IP o CIDR a escanear. Ejemplo: "10.0.0.1" o "192.168.1.0/24". */
    private String targetIp;

    /**
     * Puertos específicos a escanear. Si es null o vacío se escanean
     * los 1000 puertos más comunes (comportamiento por defecto de nmap).
     */
    private List<Integer> targetPorts;

    /** Usuario que solicitó el escaneo (email del analista/admin). */
    private String requestedBy;

    private LocalDateTime requestedAt;
}
