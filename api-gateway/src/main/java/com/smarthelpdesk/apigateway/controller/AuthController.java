package com.smarthelpdesk.apigateway.controller;

import com.smarthelpdesk.apigateway.dto.request.LoginRequest;
import com.smarthelpdesk.apigateway.dto.request.RefreshTokenRequest;
import com.smarthelpdesk.apigateway.dto.request.RegisterRequest;
import com.smarthelpdesk.apigateway.dto.response.AuthResponse;
import com.smarthelpdesk.apigateway.dto.response.RegisterResponse;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request.email(), request.password(), request.fullName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request.email(), request.password()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) throws Exception {
        AuthResponse response = authService.refresh(request.refreshToken()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request.refreshToken());

        return ResponseEntity.noContent().build();
    }

}
