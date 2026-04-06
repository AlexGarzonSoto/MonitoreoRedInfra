package com.netwatch.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para JwtTokenProvider.
 * No requiere contexto Spring: se instancia directamente con claves de prueba.
 *
 * Requisito: las claves deben tener ≥ 64 caracteres para HMAC-SHA512.
 */
class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-must-be-at-least-64-characters-for-hmac-sha512-ok!";
    private static final String REFRESH_SECRET =
            "test-refresh-key-must-be-at-least-64-characters-for-hmac-sha512-ok";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, REFRESH_SECRET, 1_800_000L, 604_800_000L);
    }

    // ── Access token ──────────────────────────────────────────────────────────

    @Test
    void generateAccessToken_generaTokenNoVacio() {
        String token = provider.generateAccessToken("user-123", "ADMIN");
        assertThat(token).isNotBlank();
    }

    @Test
    void validateAccessToken_conTokenValido_retornaTrue() {
        String token = provider.generateAccessToken("user-123", "ADMIN");
        assertThat(provider.validateAccessToken(token)).isTrue();
    }

    @Test
    void validateAccessToken_conTokenMalformado_retornaFalse() {
        assertThat(provider.validateAccessToken("esto.no.es.un.jwt")).isFalse();
    }

    @Test
    void validateAccessToken_conStringVacio_retornaFalse() {
        assertThat(provider.validateAccessToken("")).isFalse();
    }

    @Test
    void validateAccessToken_conRefreshToken_retornaFalse() {
        // Un refresh token NO debe pasar la validación de access token
        String refreshToken = provider.generateRefreshToken("user-123");
        assertThat(provider.validateAccessToken(refreshToken)).isFalse();
    }

    @Test
    void getUserIdFromToken_retornaIdCorrecto() {
        String token = provider.generateAccessToken("user-456", "ANALYST");
        assertThat(provider.getUserIdFromToken(token)).isEqualTo("user-456");
    }

    @Test
    void getRoleFromToken_retornaRolCorrecto() {
        String token = provider.generateAccessToken("user-789", "VIEWER");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("VIEWER");
    }

    @Test
    void generateAccessToken_distintosPorUsuario_noSonIguales() {
        String t1 = provider.generateAccessToken("user-A", "ADMIN");
        String t2 = provider.generateAccessToken("user-B", "ANALYST");
        assertThat(t1).isNotEqualTo(t2);
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    @Test
    void generateRefreshToken_generaTokenNoVacio() {
        String token = provider.generateRefreshToken("user-123");
        assertThat(token).isNotBlank();
    }

    @Test
    void validateRefreshToken_conTokenValido_retornaTrue() {
        String token = provider.generateRefreshToken("user-123");
        assertThat(provider.validateRefreshToken(token)).isTrue();
    }

    @Test
    void validateRefreshToken_conTokenMalformado_retornaFalse() {
        assertThat(provider.validateRefreshToken("no.es.valido")).isFalse();
    }

    @Test
    void validateRefreshToken_conAccessToken_retornaFalse() {
        // Un access token NO debe pasar la validación de refresh token
        String accessToken = provider.generateAccessToken("user-123", "ADMIN");
        assertThat(provider.validateRefreshToken(accessToken)).isFalse();
    }

    @Test
    void getUserIdFromRefreshToken_retornaIdCorrecto() {
        String token = provider.generateRefreshToken("user-refresh-999");
        assertThat(provider.getUserIdFromRefreshToken(token)).isEqualTo("user-refresh-999");
    }

    @Test
    void refreshToken_yAccessToken_sonDiferentes() {
        String access  = provider.generateAccessToken("user-X", "ADMIN");
        String refresh = provider.generateRefreshToken("user-X");
        // Firmados con claves distintas → strings distintos
        assertThat(access).isNotEqualTo(refresh);
    }
}
