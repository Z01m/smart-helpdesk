package com.smarthelpdesk.knowledgebase.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmbeddingService {
    private final RestClient client;
    private final String model;
    public EmbeddingService(RestClient embeddingRestClient, @Value("${ai.embedding-model}") String model) {
        this.client = embeddingRestClient;
        this.model = model;
    }
    public String model() { return model; }
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty() || texts.stream().anyMatch(t -> t == null || t.isBlank()))
            throw new IllegalArgumentException("Non-empty texts are required");
        var response = client.post().uri("/api/embed")
                .body(Map.of("model", model, "input", texts, "truncate", false)).retrieve().body(EmbeddingResponse.class);
        if (response == null || response.embeddings() == null || response.embeddings().size() != texts.size())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama returned an unexpected embedding count");
        try {
            for (float[] vector : response.embeddings()) VectorValues.literal(vector);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama must return non-zero 768-dimensional vectors", ex);
        }
        return response.embeddings();
    }
    public record EmbeddingResponse(List<float[]> embeddings) {}
}
