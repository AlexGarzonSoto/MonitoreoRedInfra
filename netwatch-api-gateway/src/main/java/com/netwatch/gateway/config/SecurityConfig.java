package com.netwatch.gateway.config;

import com.netwatch.gateway.security.JwtAuthFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/**").hasAnyRole("ANALYST", "ADMIN", "VIEWER")
                .requestMatchers(HttpMethod.GET, "/api/v1/alerts/**").hasAnyRole("ANALYST", "ADMIN", "VIEWER")
                .requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole("VIEWER", "ANALYST", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/remediation/**").hasAnyRole("VIEWER", "ANALYST", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/capture/**").hasAnyRole("ANALYST", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/capture/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/capture/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/scan/**").hasAnyRole("ANALYST", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/scan/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/rules/**").hasAnyRole("ANALYST", "ADMIN")
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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