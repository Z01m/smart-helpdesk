package com.smarthelpdesk.knowledgebase.dto;
import jakarta.validation.constraints.*;
public record KnowledgeSearchRequest(float[] embedding, @Size(max = 5000) String query,
        @Min(1) @Max(50) int topK) {}
