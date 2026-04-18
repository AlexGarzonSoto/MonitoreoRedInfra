package com.netwatch.gateway.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de rate limiting para proteger endpoints sensibles contra
 * ataques de fuerza bruta y abuso de API.
 *
 * <p>Algoritmo: Token Bucket (Bucket4j).
 * <ul>
 *   <li>/api/v1/auth/login  → 5 peticiones / minuto por IP</li>
 *   <li>/api/v1/auth/refresh → 10 peticiones / minuto por IP</li>
 * </ul>
 *
 * <p>El estado se mantiene en memoria (ConcurrentHashMap). En un
 * despliegue multi-instancia se debería usar Bucket4j con Redis/Valkey
 * como backend para compartir el estado entre réplicas.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // 5 tokens por minuto para el endpoint de login
    private static final int LOGIN_CAPACITY    = 5;
    private static final Duration LOGIN_PERIOD  = Duration.ofMinutes(1);

    // 10 tokens por minuto para el endpoint de refresh
    private static final int REFRESH_CAPACITY   = 10;
    private static final Duration REFRESH_PERIOD = Duration.ofMinutes(1);

    // Buckets independientes por IP para cada endpoint protegido
    private final ConcurrentHashMap<String, Bucket> loginBuckets   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.equals("/api/v1/auth/login")) {
            String ip = extractClientIp(request);
            Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> buildBucket(LOGIN_CAPACITY, LOGIN_PERIOD));
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit superado en /auth/login desde IP: {}", ip);
                sendTooManyRequests(response, LOGIN_PERIOD.toSeconds());
                return;
            }
        } else if (path.equals("/api/v1/auth/refresh")) {
            String ip = extractClientIp(request);
            Bucket bucket = refreshBuckets.computeIfAbsent(ip, k -> buildBucket(REFRESH_CAPACITY, REFRESH_PERIOD));
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit superado en /auth/refresh desde IP: {}", ip);
                sendTooManyRequests(response, REFRESH_PERIOD.toSeconds());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Construye un bucket con reposición greedy (tokens se reponen
     * continuamente durante el periodo, no de golpe al final).
     */
    private Bucket buildBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.greedy(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extrae la IP real del cliente considerando proxies (X-Forwarded-For).
     * Si hay múltiples IPs en el header, toma la primera (origen real).
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Envía respuesta 429 Too Many Requests con header Retry-After.
     */
    private void sendTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("""
                {"error":"Too Many Requests","message":"Límite de intentos superado. Intente de nuevo en %d segundos."}
                """.formatted(retryAfterSeconds));
    }

    /**
     * El filtro solo aplica a endpoints de autenticación; ignora el resto.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/auth/");
    }
}
