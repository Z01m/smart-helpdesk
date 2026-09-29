package com.smarthelpdesk.aiworker.rag;

import com.smarthelpdesk.aiworker.client.KnowledgeBaseClient;
import com.smarthelpdesk.aiworker.config.RagProperties;
import com.smarthelpdesk.aiworker.dto.knowledge.KnowledgeSearchRequest;
import com.smarthelpdesk.aiworker.dto.knowledge.KnowledgeSearchResponse;
import com.smarthelpdesk.aiworker.dto.knowledge.ScoredChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagPipeline {

    private final EmbeddingService embeddingService;
    private final KnowledgeBaseClient knowledgeBaseClient;
    private final RagProperties ragProperties;
    private final ContextBuilder contextBuilder;

    public List<ScoredChunk> retrieveContext(
            String query
    ) {
        return retrieveContext(
                query,
                ragProperties.getDefaultTopK()
        );
    }

    public List<ScoredChunk> retrieveContext(
            String query,
            int topK
    ) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "query must not be null or blank"
            );
        }

        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "topK must be greater than 0"
            );
        }

        float[] embedding =
                embeddingService.embed(query);

        KnowledgeSearchRequest request =
                new KnowledgeSearchRequest(
                        embedding,
                        topK
                );

        KnowledgeSearchResponse response =
                knowledgeBaseClient.search(
                        request
                );

        if (response.chunks() == null) {
            throw new IllegalStateException(
                    "Knowledge Base returned null chunks"
            );
        }

        return response.chunks();
    }

    public double getMaxScore(
            List<ScoredChunk> chunks
    ) {

        if (chunks == null || chunks.isEmpty()) {
            return 0.0;
        }

        return chunks.stream()
                .filter(chunk -> chunk != null)
                .mapToDouble(ScoredChunk::score)
                .max()
                .orElse(0.0);
    }

    public boolean hasRelevantContext(
            List<ScoredChunk> chunks
    ) {

        if (chunks == null || chunks.isEmpty()) {
            return false;
        }

        return getMaxScore(chunks)
                >= ragProperties.getConfidenceThreshold();
    }

    public String buildContext(
            List<ScoredChunk> chunks,
            int maxTokens
    ) {
        return contextBuilder.build(
                chunks,
                maxTokens
        );
    }

    public String buildContext(
            String query,
            int maxTokens
    ) {
        return buildContext(
                query,
                ragProperties.getDefaultTopK(),
                maxTokens
        );
    }

    public String buildContext(
            String query,
            int topK,
            int maxTokens
    ) {

        List<ScoredChunk> chunks =
                retrieveContext(
                        query,
                        topK
                );

        if (!hasRelevantContext(chunks)) {
            return "";
        }

        return contextBuilder.build(
                chunks,
                maxTokens
        );
    }
}
