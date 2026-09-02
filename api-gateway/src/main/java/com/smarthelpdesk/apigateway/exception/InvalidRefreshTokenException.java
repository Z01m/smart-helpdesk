package com.smarthelpdesk.apigateway.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super("Invalid refresh token: " + message);
    }
}
