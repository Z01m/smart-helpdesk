package com.smarthelpdesk.apigateway.dto.response;

/**
 * Ответ API с данными аутентифицированного пользователя.
 * Обычно содержит access/refresh токены и срок их действия.
 */

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn
) {
}
