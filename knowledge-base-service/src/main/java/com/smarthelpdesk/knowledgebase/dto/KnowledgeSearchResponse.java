package com.smarthelpdesk.knowledgebase.dto;
import java.util.List;
public record KnowledgeSearchResponse(List<ScoredChunk> chunks) {}
