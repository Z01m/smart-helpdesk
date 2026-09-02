package com.smarthelpdesk.apigateway.exception;

import com.smarthelpdesk.apigateway.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/v1/auth/test");
    }

    @Test
    void handleEmailAlreadyExists_shouldReturn409() {

        EmailAlreadyExistsException exception =
                new EmailAlreadyExistsException("test@example.com");

        ResponseEntity<ErrorResponse> response =
                handler.handleEmailAlreadyExists(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                409,
                response.getBody().status()
        );

        assertEquals(
                "Conflict",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/v1/auth/test",
                response.getBody().path()
        );
    }

    @Test
    void handleInvalidCredentials_shouldReturn401() {

        InvalidCredentialsException exception =
                new InvalidCredentialsException();

        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidCredentials(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                401,
                response.getBody().status()
        );

        assertEquals(
                "Unauthorized",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/v1/auth/test",
                response.getBody().path()
        );
    }

    @Test
    void handleInvalidRefreshToken_shouldReturn401() {

        InvalidRefreshTokenException exception =
                new InvalidRefreshTokenException("token is expired");

        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidRefreshToken(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                401,
                response.getBody().status()
        );

        assertEquals(
                "Unauthorized",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/v1/auth/test",
                response.getBody().path()
        );
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {

        IllegalArgumentException exception =
                new IllegalArgumentException("Invalid argument");

        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                400,
                response.getBody().status()
        );

        assertEquals(
                "Invalid argument",
                response.getBody().message()
        );
    }

    @Test
    void handleGeneric_shouldReturn500() {

        Exception exception =
                new RuntimeException("Something unexpected happened");

        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(
                        exception,
                        request
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                500,
                response.getBody().status()
        );

        assertEquals(
                "Внутренняя ошибка сервера",
                response.getBody().message()
        );
    }
}
