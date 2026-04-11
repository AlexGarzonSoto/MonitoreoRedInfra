package com.netwatch.gateway.service;

import com.netwatch.gateway.dto.LoginRequest;
import com.netwatch.gateway.dto.LoginResponse;
import com.netwatch.gateway.model.User;
import com.netwatch.gateway.repository.UserRepository;
import com.netwatch.gateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // Blacklist de refresh tokens usados: token → expiración en epoch segundos
    // Protege contra reutilización de un refresh token ya rotado.
    private final ConcurrentHashMap<String, Long> usedRefreshTokens = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Cuenta desactivada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Intento de login fallido para: {}", request.email());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId().toString());

        log.info("Login exitoso: userId={}", user.getId());
        return new LoginResponse(accessToken, refreshToken, user.getRole().name());
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        // Rechazar tokens ya usados (protección contra reutilización)
        if (usedRefreshTokens.containsKey(refreshToken)) {
            log.warn("Intento de reutilizar refresh token ya rotado");
            throw new BadCredentialsException("Refresh token ya fue utilizado");
        }

        String userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // Marcar el token actual como usado antes de emitir el nuevo
        // TTL = 7 días (604800 s) desde ahora para limpieza automática
        usedRefreshTokens.put(refreshToken, Instant.now().getEpochSecond() + 604800L);

        String newAccessToken  = jwtTokenProvider.generateAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        log.info("Refresh token rotado para userId={}", userId);
        return new LoginResponse(newAccessToken, newRefreshToken, user.getRole().name());
    }

    public void logout(String tokenHeader) {
        // Invalidar el refresh token si se envía en el header
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            String token = tokenHeader.substring(7);
            usedRefreshTokens.put(token, Instant.now().getEpochSecond() + 604800L);
        }
        log.info("Logout procesado");
    }

    /** Limpia entradas expiradas de la blacklist cada hora para evitar fuga de memoria. */
    @Scheduled(fixedRate = 3_600_000)
    public void limpiarBlacklist() {
        long ahora = Instant.now().getEpochSecond();
        int antes = usedRefreshTokens.size();
        usedRefreshTokens.entrySet().removeIf(e -> e.getValue() < ahora);
        int eliminados = antes - usedRefreshTokens.size();
        if (eliminados > 0) {
            log.debug("Blacklist de tokens: {} entradas eliminadas", eliminados);
        }
    }
}