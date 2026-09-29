package com.smarthelpdesk.aiworker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient aiRestClient(
            AiClientConfig aiClientConfig
    ) {
        return RestClient.builder()
                .baseUrl(aiClientConfig.getBaseUrl())
                .build();
    }

    @Bean("knowledgeBaseRestClient")
    public RestClient knowledgeBaseRestClient(
            KnowledgeBaseProperties properties
    ) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(
                        "X-Internal-Api-Key",
                        properties.getApiKey()
                )
                .build();
    }
}
