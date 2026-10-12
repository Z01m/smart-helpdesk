package com.smarthelpdesk.notificationservice.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
@Profile("prod")
public class ProductionSecrets {
    public ProductionSecrets(@Value("${jwt.secret}") String jwt, @Value("${internal.api-key}") String internal) {
        if (jwt.length()<48 || jwt.contains("local-development") || internal.length()<32 || internal.contains("dev-internal"))
            throw new IllegalArgumentException("Production requires independent strong JWT and internal API secrets");
        if (jwt.equals(internal)) throw new IllegalArgumentException("Use different secrets for JWT and internal API");
    }
}
