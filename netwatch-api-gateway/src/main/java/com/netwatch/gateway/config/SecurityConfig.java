package com.netwatch.gateway.config;

import com.netwatch.gateway.security.JwtAuthFilter;
import com.netwatch.gateway.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN   = "ADMIN";
    private static final String ANALYST = "ANALYST";
    private static final String VIEWER  = "VIEWER";
    private static final String CAPTURE_PATH = "/api/v1/capture/**";

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ── Headers de seguridad HTTP ─────────────────────────────────
            .headers(headers -> headers
                // Evita que el browser infiera el MIME type (MIME sniffing)
                .contentTypeOptions(c -> {})
                // Prohíbe que la app se incruste en iframes (clickjacking)
                .frameOptions(f -> f.deny())
                // Fuerza HTTPS por 1 año, aplica a subdominios
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                // CSP: solo permite recursos del mismo origen
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                // No enviar Referer a orígenes cruzados
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Deshabilitar acceso a cámara, micrófono, geolocalización
                .permissionsPolicy(p -> p
                    .policy("geolocation=(), microphone=(), camera=()"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/**").hasAnyRole(ANALYST, ADMIN, VIEWER)
                .requestMatchers(HttpMethod.GET, "/api/v1/alerts/**").hasAnyRole(ANALYST, ADMIN, VIEWER)
                .requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole(VIEWER, ANALYST, ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/remediation/**").hasAnyRole(VIEWER, ANALYST, ADMIN)
                .requestMatchers(HttpMethod.GET, CAPTURE_PATH).hasAnyRole(ANALYST, ADMIN)
                .requestMatchers(HttpMethod.PATCH, CAPTURE_PATH).hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, CAPTURE_PATH).hasRole(ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/scan/**").hasAnyRole(ANALYST, ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/v1/scan/**").hasRole(ADMIN)
                .requestMatchers("/api/v1/rules/**").hasAnyRole(ANALYST, ADMIN)
                .requestMatchers("/api/v1/users/**").hasRole(ADMIN)
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}