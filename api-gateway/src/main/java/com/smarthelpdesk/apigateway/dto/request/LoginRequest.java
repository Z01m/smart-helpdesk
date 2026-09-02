package com.smarthelpdesk.apigateway.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Данные для авторизации пользователя.
 * Содержит email и пароль, передаваемые при входе.
 */

public record LoginRequest(
        @Email @NotBlank(message = "email cannot be empty") String email,
        @NotBlank(message = "password cannot be empty")
        String password
) {
}
