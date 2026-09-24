package com.smarthelpdesk.aiworker.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "knowledge-base")
@Getter
@Setter
public class KnowledgeBaseProperties {
    private String baseUrl;
    private String apiKey;
}
