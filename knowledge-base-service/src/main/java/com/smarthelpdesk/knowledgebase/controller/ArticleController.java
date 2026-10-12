package com.smarthelpdesk.knowledgebase.controller;

import com.smarthelpdesk.knowledgebase.dto.*;
import com.smarthelpdesk.knowledgebase.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {
    private final ArticleService service;
    public ArticleController(ArticleService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/articles/" + result.id())).body(result);
    }
    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable("id") UUID id) { return service.get(id); }
    @PutMapping("/{id}")
    public ArticleResponse update(@PathVariable("id") UUID id, @Valid @RequestBody ArticleRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) { service.delete(id); }
    @PostMapping("/{id}/reindex")
    public ArticleResponse reindex(@PathVariable("id") UUID id) { return service.reindex(id); }
    @GetMapping
    public ArticlePage list(@RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "category", required = false) UUID category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) { return service.list(query, category, page, size); }
}
