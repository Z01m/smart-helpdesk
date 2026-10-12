package com.smarthelpdesk.knowledgebase.dto;
import java.util.List;
public record ArticlePage(List<ArticleResponse> content, int number, int size,
        long totalElements, long totalPages) {}
