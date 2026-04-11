package com.netwatch.scanner.service;

import com.netwatch.scanner.model.Vulnerability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Correlaciona los servicios detectados por Nmap contra la base de
 * datos NVD (National Vulnerability Database) usando la REST API v2
 * pública de NIST — sin token, sin registro.
 *
 * Endpoint: https://services.nvd.nist.gov/rest/json/cves/2.0?keywordSearch={term}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NvdCorrelationService {

    private final RestTemplate restTemplate;

    @Value("${netwatch.scanner.nvd-api-url:https://services.nvd.nist.gov/rest/json/cves/2.0}")
    private String nvdApiUrl;

    @Value("${netwatch.scanner.nvd-results-per-query:5}")
    private int resultsPerQuery;

    /**
     * Para cada línea de puerto abierto de Nmap, extrae el nombre del
     * servicio y consulta NVD. Devuelve la lista consolidada de CVEs.
     *
     * @param openPorts líneas de Nmap: "22/tcp open ssh OpenSSH 7.4p1"
     * @return CVEs encontrados, ordenados por CVSS descendente
     */
    public List<Vulnerability> correlate(List<String> openPorts) {
        List<Vulnerability> all = new ArrayList<>();

        for (String portLine : openPorts) {
            String keyword = extractSearchKeyword(portLine);
            if (keyword == null) continue;

            log.info("Consultando NVD para: '{}'", keyword);
            List<Vulnerability> cves = queryNvd(keyword, portLine);
            all.addAll(cves);
        }

        // Ordenar por CVSS descendente para mostrar los más críticos primero
        all.sort((a, b) -> Double.compare(
            b.getCvssScore() != null ? b.getCvssScore() : 0,
            a.getCvssScore() != null ? a.getCvssScore() : 0));

        return all;
    }

    private List<Vulnerability> queryNvd(String keyword, String portLine) {
        List<Vulnerability> result = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(nvdApiUrl)
                    .queryParam("keywordSearch", keyword)
                    .queryParam("resultsPerPage", resultsPerQuery)
                    .queryParam("noRejected", "")
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return result;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> vulnerabilities =
                (List<Map<String, Object>>) response.get("vulnerabilities");

            if (vulnerabilities == null) return result;

            for (Map<String, Object> item : vulnerabilities) {
                Vulnerability vuln = parseVulnerability(item, portLine);
                if (vuln != null) result.add(vuln);
            }

        } catch (RestClientException e) {
            log.warn("Error consultando NVD para '{}': {}", keyword, e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Vulnerability parseVulnerability(Map<String, Object> item, String affectedService) {
        try {
            Map<String, Object> cve = (Map<String, Object>) item.get("cve");
            if (cve == null) return null;

            String cveId = (String) cve.get("id");

            // Descripción en inglés
            String description = "";
            List<Map<String, Object>> descs =
                (List<Map<String, Object>>) cve.get("descriptions");
            if (descs != null) {
                for (Map<String, Object> d : descs) {
                    if ("en".equals(d.get("lang"))) {
                        description = truncate((String) d.get("value"), 300);
                        break;
                    }
                }
            }

            // CVSS v3.1 score y severity
            Double cvssScore = null;
            String severity = "NONE";
            Map<String, Object> metrics = (Map<String, Object>) cve.get("metrics");
            if (metrics != null) {
                List<Map<String, Object>> cvssV31 =
                    (List<Map<String, Object>>) metrics.get("cvssMetricV31");
                if (cvssV31 != null && !cvssV31.isEmpty()) {
                    Map<String, Object> cvssData =
                        (Map<String, Object>) cvssV31.get(0).get("cvssData");
                    if (cvssData != null) {
                        Object score = cvssData.get("baseScore");
                        if (score instanceof Number n) cvssScore = n.doubleValue();
                        Object sev = cvssData.get("baseSeverity");
                        if (sev instanceof String s) severity = s;
                    }
                }
            }

            return Vulnerability.builder()
                    .cveId(cveId)
                    .description(description)
                    .cvssScore(cvssScore)
                    .severity(severity)
                    .affectedService(affectedService)
                    .build();

        } catch (Exception e) {
            log.debug("Error parseando vulnerabilidad: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae el término de búsqueda más significativo de una línea Nmap.
     * Ejemplo: "22/tcp open ssh OpenSSH 7.4p1" → "OpenSSH"
     */
    private String extractSearchKeyword(String portLine) {
        // Intenta capturar el nombre del producto (4ª o 5ª columna)
        String[] parts = portLine.trim().split("\\s+");
        if (parts.length >= 4) {
            // parts[0]=port, parts[1]=state, parts[2]=service, parts[3]=product
            return parts[3];
        }
        if (parts.length == 3) {
            return parts[2]; // solo el nombre del servicio (ssh, http, etc.)
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
