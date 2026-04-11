package com.netwatch.gateway.controller;

import com.netwatch.gateway.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Genera y sirve reportes descargables de eventos y alertas
 * en formato JSON, CSV o XML.
 *
 * GET /api/v1/reports/events?format=json|csv|xml
 * GET /api/v1/reports/alerts?format=json|csv|xml
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<byte[]> eventsReport(
            @RequestParam(defaultValue = "json") String format) throws Exception {

        log.info("Descarga reporte eventos — formato: {}", format);
        String date = LocalDate.now().toString();

        return switch (format.toLowerCase()) {
            case "csv" -> buildResponse(
                    reportService.eventsAsCsv(),
                    "text/csv",
                    "netwatch-events-" + date + ".csv");
            case "xml" -> buildResponse(
                    reportService.eventsAsXml(),
                    "application/xml",
                    "netwatch-events-" + date + ".xml");
            default -> buildResponse(
                    reportService.eventsAsJson(),
                    "application/json",
                    "netwatch-events-" + date + ".json");
        };
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<byte[]> alertsReport(
            @RequestParam(defaultValue = "json") String format) throws Exception {

        log.info("Descarga reporte alertas — formato: {}", format);
        String date = LocalDate.now().toString();

        return switch (format.toLowerCase()) {
            case "csv" -> buildResponse(
                    reportService.alertsAsCsv(),
                    "text/csv",
                    "netwatch-alerts-" + date + ".csv");
            case "xml" -> buildResponse(
                    reportService.alertsAsXml(),
                    "application/xml",
                    "netwatch-alerts-" + date + ".xml");
            default -> buildResponse(
                    reportService.alertsAsJson(),
                    "application/json",
                    "netwatch-alerts-" + date + ".json");
        };
    }

    private ResponseEntity<byte[]> buildResponse(byte[] body, String mediaType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mediaType + ";charset=UTF-8"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(body.length);
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
