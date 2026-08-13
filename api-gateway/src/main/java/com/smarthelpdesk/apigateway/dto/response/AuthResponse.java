package com.smarthelpdesk.apigateway.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn
) {
}
