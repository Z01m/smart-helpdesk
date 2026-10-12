package com.smarthelpdesk.apigateway.security;
import io.jsonwebtoken.*;import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class JwtContractTest {
 final String secret="test-only-secret-that-is-long-enough-for-HMAC-and-tests-123456";
 final JwtTokenProvider jwt=new JwtTokenProvider(secret,900000,1000000);
 String custom(String subject,String type,Date expiry){var b=Jwts.builder().subject(subject).claim("type",type).claim("email","test@example.test").claim("role","CUSTOMER");if(expiry!=null)b.expiration(expiry);return b.signWith(Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();}
 @Test void validAccessRoundTrips(){UUID id=UUID.randomUUID();String token=jwt.generateAccessToken(id,"test@example.test","CUSTOMER");assertThat(jwt.validateAccessToken(token)).isTrue();assertThat(jwt.getUserId(token)).isEqualTo(id);assertThat(jwt.getExpiration(token)).isGreaterThan(System.currentTimeMillis());}
 @Test void rejectsMissingExpiry(){assertThat(jwt.validateAccessToken(custom(UUID.randomUUID().toString(),"access",null))).isFalse();}
 @Test void rejectsMalformedSubject(){assertThat(jwt.validateAccessToken(custom("not-a-uuid","access",new Date(System.currentTimeMillis()+60000)))).isFalse();}
 @Test void rejectsExpiredWrongTypeAndForgedTokens(){assertThat(jwt.validateAccessToken(custom(UUID.randomUUID().toString(),"access",new Date(0)))).isFalse();assertThat(jwt.validateAccessToken(custom(UUID.randomUUID().toString(),"refresh",new Date(System.currentTimeMillis()+60000)))).isFalse();assertThat(jwt.validateAccessToken("broken.token.signature")).isFalse();}
}
