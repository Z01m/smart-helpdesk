package com.smarthelpdesk.aiworker.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient aiRestClient(AiClientConfig aiClientConfig){
        RestClient restClient = RestClient
                .builder()
                .baseUrl(aiClientConfig.getBaseUrl())
                .build();
        return restClient;
    }

    @Bean("knowledgeBaseRestClient")
    public RestClient knowledgeBaseRestClient(KnowledgeBaseProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(
                        "X-Internal-Api-Key",
                        properties.getApiKey()
                )
                .build();
    }
}
