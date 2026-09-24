package com.smarthelpdesk.aiworker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RagProperties {

    private double confidenceThreshold = 0.75;
    private int defaultTopK = 5;
}