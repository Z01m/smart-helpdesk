package com.smarthelpdesk.aiworker.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
@Profile("prod")
public class ProductionSecrets {
    public ProductionSecrets(@Value("${knowledge-base.api-key}") String internal) {
        if (internal.length()<32 || internal.contains("dev-internal"))
            throw new IllegalArgumentException("Production requires a strong internal API secret");
    }
}
