package com.smarthelpdesk.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean("gatewayRestClient")
    RestClient gatewayRestClient(@Value("${api-gateway.base-url}") String url,
            @Value("${api-gateway.api-key}") String key) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeader("X-Internal-Api-Key", key).build();
    }
}
