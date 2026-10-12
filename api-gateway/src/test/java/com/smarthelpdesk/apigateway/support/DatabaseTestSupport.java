package com.smarthelpdesk.apigateway.support;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// Shared for the test JVM; Ryuk removes it afterwards. Never connects to development databases.
public abstract class DatabaseTestSupport {
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine");
    static { DATABASE.start(); }
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",DATABASE::getJdbcUrl);
        r.add("spring.datasource.username",DATABASE::getUsername);
        r.add("spring.datasource.password",DATABASE::getPassword);
        r.add("spring.kafka.listener.auto-startup",()->false);
        r.add("notification.delivery-delay-ms",()->3600000);
    }
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.smarthelpdesk.apigateway.kafka.OutboxEventPoller outboxPoller;
}
