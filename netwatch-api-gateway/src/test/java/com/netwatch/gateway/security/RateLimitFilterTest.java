package com.netwatch.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        // Instancia nueva por test: buckets vacíos en cada caso
        rateLimitFilter = new RateLimitFilter();
    }

    // ── /auth/login ───────────────────────────────────────────────────────────

    @Test
    void doFilter_login_primeraPeticion_pasaAlSiguiente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_login_superandoLimite_retorna429() throws Exception {
        String ip = "10.0.0.99";

        // Agotar los 5 tokens del bucket
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr(ip);
            rateLimitFilter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // La 6ª petición debe ser rechazada
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void doFilter_login_ipsDistintas_bucketsSeparados() throws Exception {
        // Agotar el bucket de IP A
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr("10.0.0.1");
            rateLimitFilter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // IP B distinta → bucket propio con tokens disponibles
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    // ── /auth/refresh ─────────────────────────────────────────────────────────

    @Test
    void doFilter_refresh_primeraPeticion_pasaAlSiguiente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setRemoteAddr("192.168.1.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_refresh_superandoLimite_retorna429() throws Exception {
        String ip = "10.0.0.88";

        // Agotar los 10 tokens del bucket de refresh
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
            req.setRemoteAddr(ip);
            rateLimitFilter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // La 11ª petición debe ser rechazada
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
    }

    // ── X-Forwarded-For ───────────────────────────────────────────────────────

    @Test
    void doFilter_conXForwardedFor_usaIpDelHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        // Primera petición desde esa IP → pasa sin problema
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_conXForwardedForMultiple_agotaBucketPorPrimeraIp() throws Exception {
        String realIp = "203.0.113.10";
        String header = realIp + ", 172.16.0.1";

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.addHeader("X-Forwarded-For", header);
            rateLimitFilter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", header);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── shouldNotFilter ───────────────────────────────────────────────────────

    @Test
    void shouldNotFilter_rutaNoAuth_retornaTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/events");
        assertThat(rateLimitFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_rutaDashboard_retornaTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts");
        assertThat(rateLimitFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_rutaLogin_retornaFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertThat(rateLimitFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_rutaRefresh_retornaFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        assertThat(rateLimitFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_cualquierRutaAuth_retornaFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        assertThat(rateLimitFilter.shouldNotFilter(request)).isFalse();
    }
}
