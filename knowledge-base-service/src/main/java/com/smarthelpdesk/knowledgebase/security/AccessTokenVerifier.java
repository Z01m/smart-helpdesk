package com.smarthelpdesk.knowledgebase.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenVerifier {
    private final SecretKey key;
    public AccessTokenVerifier(@Value("${jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public Authentication authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new IllegalArgumentException("Bearer access token is required");
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(authorization.substring(7)).getPayload();
        if (!"access".equals(claims.get("type", String.class)) || claims.getExpiration() == null)
            throw new IllegalArgumentException("An expiring access token is required");
        String user = UUID.fromString(claims.getSubject()).toString();
        String role = claims.get("role", String.class);
        if (!Set.of("CUSTOMER", "OPERATOR", "ADMIN").contains(role == null ? "" : role))
            throw new IllegalArgumentException("Unknown role");
        return new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
