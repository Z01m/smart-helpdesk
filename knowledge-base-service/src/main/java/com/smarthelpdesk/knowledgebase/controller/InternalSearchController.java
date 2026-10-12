package com.smarthelpdesk.knowledgebase.controller;
import com.smarthelpdesk.knowledgebase.dto.*;
import com.smarthelpdesk.knowledgebase.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/internal/v1/knowledge")
public class InternalSearchController {
    private final ArticleService service;
    public InternalSearchController(ArticleService service) { this.service = service; }
    @PostMapping("/search")
    public KnowledgeSearchResponse search(@Valid @RequestBody KnowledgeSearchRequest request) { return service.search(request); }
}
