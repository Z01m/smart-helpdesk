package com.smarthelpdesk.knowledgebase.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ArticleRequest(@NotBlank @Size(max = 512) String title,
        @NotBlank @Size(max = 50000) String content, UUID categoryId) {}
