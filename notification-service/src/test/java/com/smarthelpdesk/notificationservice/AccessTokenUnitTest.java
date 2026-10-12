package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.security.AccessTokenVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AccessTokenUnitTest {
    private static final String SECRET = "test-secret-0123456789012345678901234567890123456789";
    private final AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
    private String token(UUID user, String type, long expiration) {
        return "Bearer " + Jwts.builder().subject(user.toString()).claim("type", type).claim("role", "CUSTOMER")
                .expiration(new Date(expiration)).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    @Test void acceptsGatewayAccessToken() {
        UUID user = UUID.randomUUID();
        assertThat(verifier.authenticate(token(user, "access", System.currentTimeMillis() + 60000)).getName()).isEqualTo(user.toString());
    }
    @Test void rejectsRefreshAndExpiredTokens() {
        assertThatThrownBy(() -> verifier.authenticate(token(UUID.randomUUID(), "refresh", System.currentTimeMillis() + 60000)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.authenticate(token(UUID.randomUUID(), "access", System.currentTimeMillis() - 60000)))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
