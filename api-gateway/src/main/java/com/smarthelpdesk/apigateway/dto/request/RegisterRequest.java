package com.smarthelpdesk.apigateway.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Данные для регистрации нового пользователя.
 * Содержит основные регистрационные данные пользователя.
 */

public record RegisterRequest(
        @Email @NotBlank(message = "email cannot be empty") String email,
        @NotBlank(message = "password cannot be empty")
        String password,
        @NotBlank(message = "FullName cannot be empty")
        String fullName
) {
}
