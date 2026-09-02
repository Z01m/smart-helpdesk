package com.smarthelpdesk.apigateway.servise;

import com.smarthelpdesk.apigateway.dto.response.AuthResponse;
import com.smarthelpdesk.apigateway.dto.response.RegisterResponse;
import com.smarthelpdesk.apigateway.entity.RefreshToken;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import com.smarthelpdesk.apigateway.security.JwtTokenProvider;
import com.smarthelpdesk.apigateway.service.AuthService;
import com.smarthelpdesk.apigateway.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.CUSTOMER);
    }

    @Test
    void register_shouldCreateAndSaveUser() {

        String email = "new@example.com";
        String rawPassword = "password123";
        String fullName = "New User";
        String hashedPassword = "hashed-password";
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(hashedPassword);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(userId);
                    return user;
                });

        RegisterResponse result = authService.register(
                email,
                rawPassword,
                fullName
        );

        assertNotNull(result);

        assertEquals(userId, result.id());
        assertEquals(email, result.email());
        assertEquals(fullName, result.fullName());
        assertEquals(Role.CUSTOMER, result.role());
        assertEquals(CustomerTier.STANDARD, result.customerTier());

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {

        String email = "test@example.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        assertThrows(
                Exception.class,
                () -> authService.register(
                        email,
                        "password123",
                        "Test User"
                )
        );

        verify(userRepository)
                .existsByEmail(email);

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void login_shouldReturnAccessAndRefreshTokens() throws Exception {

        String email = "test@example.com";
        String rawPassword = "password123";

        String accessToken = "access-token";
        String rawRefreshToken = "refresh-token";

        RefreshToken refreshTokenEntity = new RefreshToken(
                user,
                "refresh-token-hash",
                Instant.now().plusSeconds(3600)
        );

        RefreshTokenService.CreatedRefreshToken createdRefreshToken =
                new RefreshTokenService.CreatedRefreshToken(
                        refreshTokenEntity,
                        rawRefreshToken
                );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                rawPassword,
                user.getPasswordHash()
        )).thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        )).thenReturn(accessToken);

        when(refreshTokenService.create(user))
                .thenReturn(createdRefreshToken);

        AuthResponse result = authService.login(
                email,
                rawPassword
        );

        assertNotNull(result);

        assertEquals(
                accessToken,
                result.accessToken()
        );

        assertEquals(
                rawRefreshToken,
                result.refreshToken()
        );

        assertEquals(
                900000L,
                result.expiresIn()
        );

        verify(userRepository)
                .findByEmail(email);

        verify(passwordEncoder)
                .matches(
                        rawPassword,
                        user.getPasswordHash()
                );

        verify(jwtTokenProvider)
                .generateAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                );

        verify(refreshTokenService)
                .create(user);
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {

        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception.class,
                () -> authService.login(
                        email,
                        "password123"
                )
        );

        verify(userRepository)
                .findByEmail(email);

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verifyNoInteractions(
                jwtTokenProvider,
                refreshTokenService
        );
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {

        String email = "test@example.com";
        String wrongPassword = "wrong-password";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                wrongPassword,
                user.getPasswordHash()
        )).thenReturn(false);

        assertThrows(
                Exception.class,
                () -> authService.login(
                        email,
                        wrongPassword
                )
        );

        verify(userRepository)
                .findByEmail(email);

        verify(passwordEncoder)
                .matches(
                        wrongPassword,
                        user.getPasswordHash()
                );

        verifyNoInteractions(
                jwtTokenProvider,
                refreshTokenService
        );
    }

    @Test
    void refresh_shouldRotateRefreshTokenAndReturnNewTokens()
            throws Exception {

        String oldRefreshToken = "old-refresh-token";
        String newRawRefreshToken = "new-refresh-token";
        String newAccessToken = "new-access-token";

        RefreshToken newRefreshTokenEntity = new RefreshToken(
                user,
                "new-refresh-token-hash",
                Instant.now().plusSeconds(3600)
        );

        RefreshTokenService.CreatedRefreshToken newRefreshToken =
                new RefreshTokenService.CreatedRefreshToken(
                        newRefreshTokenEntity,
                        newRawRefreshToken
                );

        when(refreshTokenService.rotateToken(oldRefreshToken))
                .thenReturn(newRefreshToken);

        when(jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        )).thenReturn(newAccessToken);

        AuthResponse result =
                authService.refresh(oldRefreshToken);

        assertNotNull(result);

        assertEquals(
                newAccessToken,
                result.accessToken()
        );

        assertEquals(
                newRawRefreshToken,
                result.refreshToken()
        );

        assertEquals(
                900000L,
                result.expiresIn()
        );

        verify(refreshTokenService)
                .rotateToken(oldRefreshToken);

        verify(jwtTokenProvider)
                .generateAccessToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                );
    }

    @Test
    void logout_shouldRevokeRefreshToken() {

        String refreshToken = "refresh-token";

        authService.logout(refreshToken);

        verify(refreshTokenService)
                .revoke(refreshToken);
    }
}