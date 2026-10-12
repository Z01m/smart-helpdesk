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
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(120000);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(aiClientConfig.getBaseUrl())
                .build();
    }

    @Bean("knowledgeBaseRestClient")
    public RestClient knowledgeBaseRestClient(
            KnowledgeBaseProperties properties
    ) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(
                        "X-Internal-Api-Key",
                        properties.getApiKey()
                )
                .build();
    }
}
