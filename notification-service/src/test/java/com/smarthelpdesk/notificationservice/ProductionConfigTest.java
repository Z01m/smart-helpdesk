package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.config.ProductionSecrets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class ProductionConfigTest {
 @ParameterizedTest @ValueSource(strings={"", "short", "your-very-long-secret-key-for-local-development-123456789"})
 void prodRejectsWeakOrDevelopmentJwt(String jwt){assertThatThrownBy(()->new ProductionSecrets(jwt,"separate-internal-secret-with-at-least-32-characters")).isInstanceOf(IllegalArgumentException.class);}
 @Test void prodRejectsReusedSecret(){String secret="same-secret-for-two-purposes-is-forbidden-123456789012345";assertThatThrownBy(()->new ProductionSecrets(secret,secret)).isInstanceOf(IllegalArgumentException.class);}
 @Test void prodAcceptsIndependentConfiguredSecrets(){new ProductionSecrets("test-jwt-secret-with-more-than-forty-eight-characters-123456","test-internal-secret-with-more-than-thirty-two-characters");}
}
