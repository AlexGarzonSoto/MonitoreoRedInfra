package com.netwatch.osint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netwatch.osint.dto.GeoIpData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Consulta geolocalización de IPs usando ip-api.com (gratuito, sin API key,
 * límite de 45 req/min en el plan free).
 *
 * Los resultados se cachean en Valkey (compatible con Redis) con TTL de 1 hora
 * para respetar el rate-limit y reducir latencia en IPs recurrentes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoIpService {

    private static final String IP_API_URL     =
            "http://ip-api.com/json/{ip}?fields=status,country,city,lat,lon,as,query";
    private static final String CACHE_PREFIX   = "geoip:";
    private static final Duration CACHE_TTL    = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final RestClient.Builder  restClientBuilder;
    private final ObjectMapper        objectMapper;

    /**
     * Busca la geolocalización de la IP dada.
     * 1. Intenta obtener del caché Valkey.
     * 2. Si no está, consulta ip-api.com y guarda en caché.
     * 3. Si falla, retorna un resultado vacío sin lanzar excepción.
     */
    public GeoIpData lookup(String ip) {
        // 1. Consultar caché
        String cacheKey = CACHE_PREFIX + ip;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit GeoIP: {}", ip);
                return objectMapper.readValue(cached, GeoIpData.class);
            }
        } catch (Exception e) {
            log.warn("Error leyendo caché GeoIP para {}: {}", ip, e.getMessage());
        }

        // 2. Consultar ip-api.com
        try {
            GeoIpData data = restClientBuilder.build()
                    .get()
                    .uri(IP_API_URL, ip)
                    .retrieve()
                    .body(GeoIpData.class);

            if (data == null || !data.isSuccess()) {
                log.warn("ip-api.com no resolvió la IP: {}", ip);
                return GeoIpData.unknown(ip);
            }

            // 3. Guardar en caché
            try {
                redisTemplate.opsForValue().set(
                        cacheKey, objectMapper.writeValueAsString(data), CACHE_TTL);
            } catch (Exception e) {
                log.warn("Error guardando en caché GeoIP para {}: {}", ip, e.getMessage());
            }

            log.debug("GeoIP resuelto: {} → {}, {}", ip, data.country(), data.city());
            return data;

        } catch (Exception e) {
            log.error("Error consultando ip-api.com para {}: {}", ip, e.getMessage());
            return GeoIpData.unknown(ip);
        }
    }
}
