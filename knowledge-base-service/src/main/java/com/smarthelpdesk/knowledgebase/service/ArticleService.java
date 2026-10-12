package com.smarthelpdesk.knowledgebase.service;

import com.smarthelpdesk.knowledgebase.dto.*;
import com.smarthelpdesk.knowledgebase.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ArticleService {
    private final ArticleRepository repository;
    private final ChunkingService chunking;
    private final EmbeddingService embeddings;
    public ArticleService(ArticleRepository repository, ChunkingService chunking, EmbeddingService embeddings) {
        this.repository = repository; this.chunking = chunking; this.embeddings = embeddings;
    }
    public ArticleResponse create(ArticleRequest request) { return index(UUID.randomUUID(), request, null); }
    public ArticleResponse update(UUID id, ArticleRequest request) { return index(id, request, repository.get(id).version()); }
    public ArticleResponse reindex(UUID id) {
        var article = repository.get(id);
        return index(id, new ArticleRequest(article.title(), article.content(), article.categoryId()), article.version());
    }
    private ArticleResponse index(UUID id, ArticleRequest request, Long version) {
        var chunks = chunking.split(request.content());
        var vectors = embeddings.embed(chunks.stream().map(text -> request.title() + "\n" + text).toList());
        // Network I/O finishes before opening the short database transaction.
        return repository.save(id, request, chunks, vectors, embeddings.model(), version);
    }
    public ArticleResponse get(UUID id) { return repository.get(id); }
    public void delete(UUID id) { repository.delete(id); }
    public ArticlePage list(String query, UUID category, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page >= 0 and size between 1 and 100 required");
        if (query != null && query.length() > 5000) throw new IllegalArgumentException("Query is too long");
        return repository.list(query, category, page, size);
    }
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        if (request.topK() < 1 || request.topK() > 50) throw new IllegalArgumentException("topK must be between 1 and 50");
        boolean hasQuery = request.query() != null && !request.query().isBlank();
        if ((request.embedding() != null) == hasQuery) throw new IllegalArgumentException("Supply exactly one of embedding or query");
        float[] vector = request.embedding() != null ? request.embedding() : embeddings.embed(List.of(request.query())).getFirst();
        return new KnowledgeSearchResponse(repository.search(vector, request.topK(), embeddings.model()));
    }
}
