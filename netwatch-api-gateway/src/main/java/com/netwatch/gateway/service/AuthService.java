package com.netwatch.gateway.service;

import com.netwatch.gateway.dto.LoginRequest;
import com.netwatch.gateway.dto.LoginResponse;
import com.netwatch.gateway.model.User;
import com.netwatch.gateway.repository.UserRepository;
import com.netwatch.gateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

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
        // Validar refresh token y emitir nuevo access token
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        String userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                userId, user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        return new LoginResponse(newAccessToken, newRefreshToken, user.getRole().name());
    }

    public void logout(String tokenHeader) {
        // En producción: agregar token a blacklist en Redis
        log.info("Logout procesado");
    }
}