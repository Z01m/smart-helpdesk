package com.smarthelpdesk.apigateway.dto.response;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String code,
        String message,
        String path,
        String correlationId
) {
}
