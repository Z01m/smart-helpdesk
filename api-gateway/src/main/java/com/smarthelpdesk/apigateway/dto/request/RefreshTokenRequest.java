package com.smarthelpdesk.apigateway.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Данные для обновления access-токена.
 * Содержит refresh-токен пользователя.
 */

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken cannot be empty")
        String refreshToken
) {
}
