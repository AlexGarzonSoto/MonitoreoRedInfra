package com.netwatch.gateway.model;

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

    private String scanId;
    private String targetIp;
    private List<Integer> targetPorts;
    private String requestedBy;
    private LocalDateTime requestedAt;
}
