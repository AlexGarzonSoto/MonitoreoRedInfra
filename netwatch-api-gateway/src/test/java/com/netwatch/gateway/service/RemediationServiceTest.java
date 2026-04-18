package com.netwatch.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemediationServiceTest {

    @Mock private RestTemplate restTemplate;

    @InjectMocks private RemediationService remediationService;

    // ── getRemediation ────────────────────────────────────────────────────────

    @Test
    void getRemediation_portScan_retornaInfoCorrecta() {
        RemediationService.RemediationInfo info = remediationService.getRemediation("PORT_SCAN");

        assertThat(info.threatType()).isEqualTo("PORT_SCAN");
        assertThat(info.mitreTechnique()).contains("T1046");
        assertThat(info.remediationSteps()).isNotEmpty();
        assertThat(info.references()).isNotEmpty();
    }

    @Test
    void getRemediation_bruteForce_retornaInfoCorrecta() {
        RemediationService.RemediationInfo info = remediationService.getRemediation("BRUTE_FORCE");

        assertThat(info.threatType()).isEqualTo("BRUTE_FORCE");
        assertThat(info.mitreTechnique()).contains("T1110");
        assertThat(info.riskLevel()).isEqualTo("CRITICAL");
    }

    @Test
    void getRemediation_synFlood_retornaInfoCorrecta() {
        RemediationService.RemediationInfo info = remediationService.getRemediation("SYN_FLOOD");

        assertThat(info.threatType()).isEqualTo("SYN_FLOOD");
        assertThat(info.remediationSteps()).hasSizeGreaterThan(3);
    }

    @Test
    void getRemediation_dnsTunneling_retornaInfoCorrecta() {
        RemediationService.RemediationInfo info = remediationService.getRemediation("DNS_TUNNELING");

        assertThat(info.threatType()).isEqualTo("DNS_TUNNELING");
        assertThat(info.mitreTechnique()).contains("T1071");
    }

    @Test
    void getRemediation_tipoDesconocido_retornaNormal() {
        RemediationService.RemediationInfo info = remediationService.getRemediation("TIPO_INEXISTENTE");

        assertThat(info.threatType()).isEqualTo("NORMAL");
    }

    @Test
    void getRemediation_null_retornaNormal() {
        RemediationService.RemediationInfo info = remediationService.getRemediation(null);

        assertThat(info.threatType()).isEqualTo("NORMAL");
    }

    @Test
    void getRemediation_minusculas_funcionaIgual() {
        RemediationService.RemediationInfo info = remediationService.getRemediation("port_scan");

        assertThat(info.threatType()).isEqualTo("PORT_SCAN");
    }

    // ── getAllRemediations ────────────────────────────────────────────────────

    @Test
    void getAllRemediations_retornaTodasLasEntradas() {
        List<RemediationService.RemediationInfo> all = remediationService.getAllRemediations();

        assertThat(all).isNotEmpty();
        assertThat(all).extracting(RemediationService.RemediationInfo::threatType)
                .contains("PORT_SCAN", "BRUTE_FORCE", "SYN_FLOOD", "DNS_TUNNELING", "NORMAL");
    }

    // ── queryCves ─────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void queryCves_conRespuestaValida_retornaCves() {
        Map<String, Object> cveData = Map.of(
            "id", "CVE-2021-44228",
            "descriptions", List.of(Map.of("lang", "en", "value", "Log4Shell RCE vulnerability"))
        );
        Map<String, Object> vuln    = Map.of("cve", cveData);
        Map<String, Object> nvdResp = Map.of("vulnerabilities", List.of(vuln));

        when(restTemplate.getForObject(anyString(), eq(Map.class), anyString()))
                .thenReturn(nvdResp);

        List<Map<String, String>> cves = remediationService.queryCves("PORT_SCAN");

        assertThat(cves).hasSize(1);
        assertThat(cves.get(0).get("id")).isEqualTo("CVE-2021-44228");
    }

    @Test
    void queryCves_tipoDesconocido_retornaListaVacia() {
        List<Map<String, String>> cves = remediationService.queryCves("TIPO_RARO");

        assertThat(cves).isEmpty();
    }

    @Test
    void queryCves_conErrorDeRed_retornaListaVacia() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), anyString()))
                .thenThrow(new RestClientException("timeout"));

        List<Map<String, String>> cves = remediationService.queryCves("BRUTE_FORCE");

        assertThat(cves).isEmpty();
    }

    @Test
    void queryCves_conRespuestaNula_retornaListaVacia() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), anyString()))
                .thenReturn(null);

        List<Map<String, String>> cves = remediationService.queryCves("SYN_FLOOD");

        assertThat(cves).isEmpty();
    }
}
