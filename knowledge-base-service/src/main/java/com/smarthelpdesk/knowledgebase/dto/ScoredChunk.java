package com.smarthelpdesk.knowledgebase.dto;
import java.util.UUID;
public record ScoredChunk(UUID articleId, String title, String text, double score) {}
