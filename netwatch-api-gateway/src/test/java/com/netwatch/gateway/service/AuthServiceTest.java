package com.netwatch.gateway.service;

import com.netwatch.gateway.dto.LoginRequest;
import com.netwatch.gateway.dto.LoginResponse;
import com.netwatch.gateway.model.User;
import com.netwatch.gateway.repository.UserRepository;
import com.netwatch.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User activeUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(userId)
                .email("analista@netwatch.local")
                .passwordHash("$2a$12$hashedPassword")
                .role(User.Role.ANALYST)
                .active(true)
                .build();
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_conCredencialesValidas_retornaTokens() {
        LoginRequest request = new LoginRequest("analista@netwatch.local", "NetWatch2024!");

        when(userRepository.findByEmail("analista@netwatch.local"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("NetWatch2024!", "$2a$12$hashedPassword"))
                .thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId.toString(), "ANALYST"))
                .thenReturn("access.token.value");
        when(jwtTokenProvider.generateRefreshToken(userId.toString()))
                .thenReturn("refresh.token.value");

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access.token.value");
        assertThat(response.refreshToken()).isEqualTo("refresh.token.value");
        assertThat(response.role()).isEqualTo("ANALYST");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_conPasswordIncorrecta_lanzaBadCredentials() {
        LoginRequest request = new LoginRequest("analista@netwatch.local", "claveIncorrecta");

        when(userRepository.findByEmail("analista@netwatch.local"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("claveIncorrecta", "$2a$12$hashedPassword"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtTokenProvider, never()).generateAccessToken(any(), any());
    }

    @Test
    void login_conEmailNoExistente_lanzaBadCredentials() {
        LoginRequest request = new LoginRequest("noexiste@netwatch.local", "NetWatch2024!");

        when(userRepository.findByEmail("noexiste@netwatch.local"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_conCuentaInactiva_lanzaBadCredentials() {
        activeUser.setActive(false);
        LoginRequest request = new LoginRequest("analista@netwatch.local", "NetWatch2024!");

        when(userRepository.findByEmail("analista@netwatch.local"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("desactivada");
    }

    @Test
    void login_comoAdmin_retornaRolAdmin() {
        activeUser.setRole(User.Role.ADMIN);
        LoginRequest request = new LoginRequest("analista@netwatch.local", "NetWatch2024!");

        when(userRepository.findByEmail("analista@netwatch.local"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId.toString(), "ADMIN"))
                .thenReturn("admin.access.token");
        when(jwtTokenProvider.generateRefreshToken(userId.toString()))
                .thenReturn("admin.refresh.token");

        LoginResponse response = authService.login(request);

        assertThat(response.role()).isEqualTo("ADMIN");
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_conTokenValido_retornaTokensNuevos() {
        when(jwtTokenProvider.validateRefreshToken("valid.refresh.token"))
                .thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken("valid.refresh.token"))
                .thenReturn(userId.toString());
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(userId.toString(), "ANALYST"))
                .thenReturn("new.access.token");
        when(jwtTokenProvider.generateRefreshToken(userId.toString()))
                .thenReturn("new.refresh.token");

        LoginResponse response = authService.refresh("valid.refresh.token");

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.refreshToken()).isEqualTo("new.refresh.token");
    }

    @Test
    void refresh_conTokenInvalido_lanzaBadCredentials() {
        when(jwtTokenProvider.validateRefreshToken("token.invalido"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("token.invalido"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void refresh_conUsuarioNoEncontrado_lanzaBadCredentials() {
        when(jwtTokenProvider.validateRefreshToken("orphan.refresh.token"))
                .thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken("orphan.refresh.token"))
                .thenReturn(userId.toString());
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("orphan.refresh.token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_noLanzaExcepcion() {
        // logout solo registra en log — no debe lanzar excepción
        assertThatNoException().isThrownBy(() -> authService.logout("Bearer some.valid.token"));
    }
}
