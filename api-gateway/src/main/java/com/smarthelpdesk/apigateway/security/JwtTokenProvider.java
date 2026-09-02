package com.smarthelpdesk.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
/**
 * Компонент для работы с JWT-токенами.
 * Создаёт, проверяет токены и извлекает из них данные пользователя.
 */
@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpiration;


    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        return generateToken(userId,email,role,accessTokenExpiration);
    }

    private String generateToken(UUID userId, String email, String role, long expiration){
        Date now = new Date();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration( new Date(now.getTime() + expiration))
                .claim("type","access")
                .signWith(secretKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = getClaims(token);

            String tokenType = claims.get("type", String.class);

            return "access".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getUserId(String token) {
        Claims claims = getClaims(token);

        return UUID.fromString(
                claims.getSubject()
        );
    }

    public String getEmail(String token) {
        return getClaims(token)
                .get("email", String.class);
    }

    public String getRole(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    public long getExpiration(String token) {
        Date expiration = getClaims(token)
                .getExpiration();

        return expiration.getTime();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }



}
