package com.smarthelpdesk.knowledgebase.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration
public class RestClientConfig {
    @Bean
    RestClient embeddingRestClient(@Value("${ai.base-url}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(60000);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
}
