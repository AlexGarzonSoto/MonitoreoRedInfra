package com.netwatch.scanner.consumer;

import com.netwatch.scanner.config.RabbitMQConfig;
import com.netwatch.scanner.model.ScanRequestMessage;
import com.netwatch.scanner.model.ScanResult;
import com.netwatch.scanner.model.Vulnerability;
import com.netwatch.scanner.service.NmapScannerService;
import com.netwatch.scanner.service.NvdCorrelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consume solicitudes de escaneo desde la cola {@code netwatch.scan.requests},
 * ejecuta Nmap + correlación NVD y publica el resultado en
 * {@code netwatch.scan.results} para que el api-gateway lo sirva al cliente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScanRequestConsumer {

    private final NmapScannerService nmapScannerService;
    private final NvdCorrelationService nvdCorrelationService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
        queues = RabbitMQConfig.SCAN_REQ_QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void procesarSolicitud(ScanRequestMessage request) {
        log.info("Escaneo iniciado — id={} target={} solicitado por={}",
            request.getScanId(), request.getTargetIp(), request.getRequestedBy());

        ScanResult result;
        try {
            // 1. Ejecutar Nmap
            List<String> openPorts = nmapScannerService.scan(
                request.getTargetIp(), request.getTargetPorts());

            // 2. Correlacionar con NVD
            List<Vulnerability> vulnerabilities = nvdCorrelationService.correlate(openPorts);

            log.info("Escaneo id={} → {} puertos abiertos, {} CVEs encontrados",
                request.getScanId(), openPorts.size(), vulnerabilities.size());

            result = ScanResult.builder()
                    .scanId(request.getScanId())
                    .targetIp(request.getTargetIp())
                    .openPorts(openPorts)
                    .vulnerabilities(vulnerabilities)
                    .status("COMPLETED")
                    .scannedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error en escaneo id={}: {}", request.getScanId(), e.getMessage());
            result = ScanResult.builder()
                    .scanId(request.getScanId())
                    .targetIp(request.getTargetIp())
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .scannedAt(LocalDateTime.now())
                    .build();
        }

        // 3. Publicar resultado para que el api-gateway lo recoja
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.RK_SCAN_RESULT,
            result
        );
    }
}
