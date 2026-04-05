package com.netwatch.gateway.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String role
) {
    public LoginResponse(String accessToken, String refreshToken, String role) {
        this(accessToken, refreshToken, "Bearer", 1800, role);
    }
}