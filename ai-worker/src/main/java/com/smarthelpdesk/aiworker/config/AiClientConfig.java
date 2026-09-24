package com.smarthelpdesk.aiworker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiClientConfig {

    private String baseUrl;

    private String model;
    private String embeddingModel;

    private double classificationTemperature;
    private double answerTemperature;

    private double classificationTopP;
    private double answerTopP;
}