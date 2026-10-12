package com.smarthelpdesk.knowledgebase.dto;
import java.time.Instant;
import java.util.UUID;
public record ArticleResponse(UUID id, String title, String content, UUID categoryId,
        String category, Instant createdAt, Instant updatedAt, long version) {}
