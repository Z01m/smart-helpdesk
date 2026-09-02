package com.smarthelpdesk.apigateway.dto.response;

import java.time.Instant;

/**
 * Единый формат ответа при возникновении ошибки.
 * Используется для возврата клиенту HTTP-кода и описания ошибки.
 */

public record ErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path
) {
}
