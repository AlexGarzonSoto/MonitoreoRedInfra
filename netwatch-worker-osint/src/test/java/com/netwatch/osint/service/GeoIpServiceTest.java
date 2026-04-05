package com.netwatch.osint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netwatch.osint.dto.GeoIpData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoIpServiceTest {

    @Mock StringRedisTemplate   redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock RestClient.Builder    restClientBuilder;

    private GeoIpService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new GeoIpService(redisTemplate, restClientBuilder, objectMapper);
    }

    @Test
    void lookup_cacheHit_returnsCachedData() throws Exception {
        GeoIpData cached = new GeoIpData("success", "Colombia", "Bogotá",
                4.6, -74.08, "AS12345 ISP Example", "1.2.3.4");
        when(valueOps.get("geoip:1.2.3.4"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        GeoIpData result = service.lookup("1.2.3.4");

        assertThat(result.country()).isEqualTo("Colombia");
        assertThat(result.city()).isEqualTo("Bogotá");
        verify(restClientBuilder, never()).build();
    }

    @Test
    void lookup_redisError_doesNotPropagateException() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        // RestClient no está configurado, lanzará excepción → retorna unknown
        GeoIpData result = service.lookup("1.2.3.4");

        assertThat(result.status()).isEqualTo("fail");
        assertThat(result.query()).isEqualTo("1.2.3.4");
    }

    @Test
    void geoIpData_unknown_returnsFailStatus() {
        GeoIpData unknown = GeoIpData.unknown("9.9.9.9");

        assertThat(unknown.isSuccess()).isFalse();
        assertThat(unknown.country()).isEqualTo("Unknown");
        assertThat(unknown.query()).isEqualTo("9.9.9.9");
    }

    @Test
    void geoIpData_success_isSuccessTrue() {
        GeoIpData success = new GeoIpData("success", "Colombia", "Bogotá",
                4.6, -74.08, "AS12345", "1.2.3.4");

        assertThat(success.isSuccess()).isTrue();
    }
}
