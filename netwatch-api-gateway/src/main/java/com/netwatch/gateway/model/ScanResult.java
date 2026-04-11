package com.netwatch.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanResult {

    private String scanId;
    private String targetIp;
    private List<String> openPorts;
    private List<Map<String, Object>> vulnerabilities;
    private String status;
    private String errorMessage;
    private LocalDateTime scannedAt;
}
