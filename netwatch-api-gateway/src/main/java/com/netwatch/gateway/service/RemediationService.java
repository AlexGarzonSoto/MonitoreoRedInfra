package com.netwatch.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Proporciona información de remediación para cada tipo de amenaza.
 * Incluye:
 *  - Técnica MITRE ATT&CK
 *  - Pasos de remediación
 *  - CVEs relacionados (consulta NVD API si está disponible)
 *  - Referencias externas
 */
@Service
@Slf4j
public class RemediationService {

    private final RestTemplate restTemplate;

    @Value("${netwatch.nvd.api.key:}")
    private String nvdApiKey;

    private static final String NVD_URL =
        "https://services.nvd.nist.gov/rest/json/cves/2.0?keywordSearch={kw}&resultsPerPage=3";

    public RemediationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── Base de conocimiento local ────────────────────────────────────────────

    private static final String T_PORT_SCAN    = "PORT_SCAN";
    private static final String T_BRUTE_FORCE  = "BRUTE_FORCE";
    private static final String T_SYN_FLOOD    = "SYN_FLOOD";
    private static final String T_DNS_TUNNELING = "DNS_TUNNELING";
    private static final String T_NORMAL       = "NORMAL";

    private static final Map<String, RemediationInfo> KB = new LinkedHashMap<>();

    static {
        KB.put(T_PORT_SCAN, new RemediationInfo(
            T_PORT_SCAN,
            "Escaneo de Puertos",
            "T1046 — Network Service Discovery",
            "HIGH",
            "Un atacante está enumerando puertos abiertos para identificar servicios vulnerables.",
            List.of(
                "Implementar reglas de firewall que limiten el número de conexiones por segundo por IP (rate limiting).",
                "Activar IDS/IPS (Snort, Suricata) con reglas de detección de escaneo.",
                "Deshabilitar todos los puertos y servicios que no sean necesarios.",
                "Usar port knocking para servicios sensibles como SSH.",
                "Revisar los logs del firewall para identificar el origen y bloquear la IP atacante.",
                "Considerar honeypots en puertos comúnmente escaneados (21, 23, 8080)."
            ),
            List.of(
                "CVE-2021-44228 — Log4Shell: descubierto vía escaneo de puertos masivo",
                "CVE-2020-0796 — SMBGhost: explotado tras descubrimiento por escaneo"
            ),
            List.of(
                "https://attack.mitre.org/techniques/T1046/",
                "https://www.cisa.gov/news-events/cybersecurity-advisories",
                "https://nvd.nist.gov/vuln/search?query=port+scanning"
            )
        ));

        KB.put(T_BRUTE_FORCE, new RemediationInfo(
            T_BRUTE_FORCE,
            "Fuerza Bruta",
            "T1110 — Brute Force",
            "CRITICAL",
            "Se detectaron múltiples intentos de autenticación fallidos, indicando un ataque de fuerza bruta a credenciales.",
            List.of(
                "Implementar Account Lockout: bloquear cuenta tras 5 intentos fallidos durante 15 minutos.",
                "Habilitar autenticación multifactor (MFA/2FA) en todos los servicios expuestos.",
                "Cambiar puertos por defecto de servicios críticos (SSH a puerto no estándar > 1024).",
                "Usar fail2ban o similares para bloquear automáticamente IPs con múltiples fallos.",
                "Deshabilitar login de root remoto (PermitRootLogin no en /etc/ssh/sshd_config).",
                "Implementar autenticación por clave pública en SSH, deshabilitar passwords.",
                "Revisar y rotar credenciales si el ataque fue exitoso."
            ),
            List.of(
                "CVE-2023-32784 — KeePass: extracción de contraseña maestra",
                "CVE-2022-26923 — Active Directory: escalada de privilegios vía fuerza bruta",
                "CVE-2021-21985 — VMware vCenter: ataques de fuerza bruta expuestos"
            ),
            List.of(
                "https://attack.mitre.org/techniques/T1110/",
                "https://owasp.org/www-community/controls/Blocking_Brute_Force_Attacks",
                "https://nvd.nist.gov/vuln/search?query=brute+force+authentication"
            )
        ));

        KB.put(T_SYN_FLOOD, new RemediationInfo(
            T_SYN_FLOOD,
            "Inundación SYN (DoS)",
            "T1498.001 — Network Denial of Service: Direct Network Flood",
            "CRITICAL",
            "Se detecta un volumen anormal de paquetes SYN sin completar el handshake TCP. Ataque de denegación de servicio.",
            List.of(
                "Habilitar SYN Cookies en el kernel (sysctl -w net.ipv4.tcp_syncookies=1).",
                "Reducir el timeout de conexiones semi-abiertas (net.ipv4.tcp_synack_retries=2).",
                "Aumentar la cola de conexiones en espera (net.ipv4.tcp_max_syn_backlog=4096).",
                "Configurar límites de tasa en iptables: iptables -A INPUT -p tcp --syn -m limit --limit 1/s -j ACCEPT.",
                "Activar scrubbing de tráfico en el proveedor de Internet (upstream mitigation).",
                "Implementar Anycast y CDN para distribuir el tráfico de ataque.",
                "Considerar soluciones DDoS como Cloudflare, AWS Shield o Akamai.",
                "Documentar el incidente y reportar la IP de origen a las autoridades (CSIRT)."
            ),
            List.of(
                "CVE-2019-11477 — TCP SACK Panic: vulnerabilidad del kernel relacionada",
                "CVE-2018-5390 — SegmentSmack: DoS en kernel Linux via paquetes TCP",
                "CVE-2016-5696 — Off-Path TCP Exploits"
            ),
            List.of(
                "https://attack.mitre.org/techniques/T1498/001/",
                "https://www.cisa.gov/sites/default/files/publications/understanding-and-responding-to-ddos-attacks_508c.pdf",
                "https://nvd.nist.gov/vuln/search?query=SYN+flood+denial+service"
            )
        ));

        KB.put(T_DNS_TUNNELING, new RemediationInfo(
            T_DNS_TUNNELING,
            "Tunneling DNS",
            "T1071.004 — Application Layer Protocol: DNS",
            "HIGH",
            "Se detectaron paquetes DNS con payload inusualmente grande (>512 bytes), posible exfiltración de datos vía DNS.",
            List.of(
                "Inspeccionar el contenido de consultas DNS en busca de datos codificados en base64 o hex.",
                "Limitar el tamaño de paquetes UDP DNS a 512 bytes en el firewall.",
                "Implementar DNS Firewall / RPZ (Response Policy Zones) para bloquear dominios sospechosos.",
                "Usar un resolver DNS interno y bloquear consultas DNS directas al exterior (solo permitir al resolver corporativo).",
                "Monitorear volumen de consultas DNS por host: anomalías indican tunneling.",
                "Analizar los dominios consultados con herramientas de threat intelligence (VirusTotal, PassiveDNS).",
                "Implementar DLP (Data Loss Prevention) en la capa de red.",
                "Revisar herramientas instaladas: iodine, dnscat2, Cobalt Strike DNS beacon."
            ),
            List.of(
                "CVE-2020-1350 — SIGRed: vulnerabilidad crítica en Windows DNS Server",
                "CVE-2021-26855 — Exchange SSRF: exfiltración via DNS tunneling",
                "CVE-2019-6471 — BIND: amplificación DNS"
            ),
            List.of(
                "https://attack.mitre.org/techniques/T1071/004/",
                "https://www.cisa.gov/news-events/alerts/2019/04/26/dns-infrastructure-tampering",
                "https://nvd.nist.gov/vuln/search?query=dns+tunneling+exfiltration"
            )
        ));

        KB.put(T_NORMAL, new RemediationInfo(
            T_NORMAL,
            "Tráfico Normal",
            "N/A",
            "INFO",
            "El tráfico analizado no presenta indicadores de compromiso (IoC). No se requiere acción inmediata.",
            List.of(
                "Mantener monitoreo continuo como práctica de seguridad preventiva.",
                "Revisar periódicamente las políticas de acceso y listas de control (ACL).",
                "Actualizar las firmas del IDS/IPS regularmente."
            ),
            Collections.emptyList(),
            List.of("https://www.cisa.gov/topics/cybersecurity-best-practices")
        ));
    }

    // ── API pública ──────────────────────────────────────────────────────────

    public RemediationInfo getRemediation(String threatType) {
        String key = threatType != null ? threatType.toUpperCase() : T_NORMAL;
        return KB.getOrDefault(key, KB.get(T_NORMAL));
    }

    public List<RemediationInfo> getAllRemediations() {
        return new ArrayList<>(KB.values());
    }

    /**
     * Consulta la API de NVD para obtener CVEs relacionados con el tipo de amenaza.
     * Retorna lista vacía si no hay API key o si falla la consulta.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> queryCves(String threatType) {
        String kw = switch (threatType.toUpperCase()) {
            case T_PORT_SCAN    -> "network port scanning";
            case T_BRUTE_FORCE  -> "brute force authentication";
            case T_SYN_FLOOD    -> "SYN flood denial of service";
            case T_DNS_TUNNELING -> "DNS tunneling exfiltration";
            default             -> "";
        };
        if (kw.isBlank()) return Collections.emptyList();

        try {
            var response = restTemplate.getForObject(NVD_URL, Map.class, kw);
            if (response == null) return Collections.emptyList();

            List<Map<String, Object>> vulns = (List<Map<String, Object>>) response.get("vulnerabilities");
            if (vulns == null) return Collections.emptyList();

            return vulns.stream()
                .limit(3)
                .map(v -> {
                    try {
                        Map<String, Object> cve = (Map<String, Object>) v.get("cve");
                        String id = (String) cve.get("id");
                        List<Map<String, Object>> descs = (List<Map<String, Object>>) cve.get("descriptions");
                        String desc = descs.stream()
                            .filter(d -> "en".equals(d.get("lang")))
                            .findFirst()
                            .map(d -> (String) d.get("value"))
                            .orElse("Sin descripción");
                        return Map.of("id", id, "description", desc.length() > 200 ? desc.substring(0, 200) + "..." : desc);
                    } catch (Exception ex) {
                        return Map.<String, String>of();
                    }
                })
                .filter(m -> !m.isEmpty())
                .toList();
        } catch (Exception e) {
            log.debug("NVD API no disponible para '{}': {}", threatType, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── DTO interno ──────────────────────────────────────────────────────────

    public record RemediationInfo(
        String threatType,
        String threatName,
        String mitreTechnique,
        String riskLevel,
        String description,
        List<String> remediationSteps,
        List<String> relatedCves,
        List<String> references
    ) {}
}
