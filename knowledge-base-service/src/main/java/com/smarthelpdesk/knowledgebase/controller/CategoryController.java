package com.smarthelpdesk.knowledgebase.controller;
import com.smarthelpdesk.knowledgebase.dto.CategoryResponse;
import com.smarthelpdesk.knowledgebase.repository.ArticleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final ArticleRepository repository;
    public CategoryController(ArticleRepository repository) { this.repository = repository; }
    @GetMapping
    public List<CategoryResponse> list() { return repository.categories(); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) { return repository.createCategory(request.name()); }
    public record CategoryRequest(@NotBlank @Size(max = 128) String name) {}
}
