package com.smarthelpdesk.aiworker.client;

import com.smarthelpdesk.aiworker.dto.knowledge.KnowledgeSearchRequest;
import com.smarthelpdesk.aiworker.dto.knowledge.KnowledgeSearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KnowledgeBaseClient {

    private final RestClient restClient;

    public KnowledgeBaseClient(
            @Qualifier("knowledgeBaseRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public KnowledgeSearchResponse search(
            KnowledgeSearchRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "request cannot be null"
            );
        }

        if (request.embedding() == null
                || request.embedding().length == 0) {

            throw new IllegalArgumentException(
                    "embedding cannot be null or empty"
            );
        }

        if (request.topK() <= 0) {
            throw new IllegalArgumentException(
                    "topK must be greater than 0"
            );
        }

        KnowledgeSearchResponse response =
                restClient.post()
                        .uri("/internal/v1/knowledge/search")
                        .body(request)
                        .retrieve()
                        .body(KnowledgeSearchResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Knowledge Base returned null response"
            );
        }

        return response;
    }
}
