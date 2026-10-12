package com.smarthelpdesk.knowledgebase.repository;

import com.smarthelpdesk.knowledgebase.dto.*;
import com.smarthelpdesk.knowledgebase.service.VectorValues;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Repository
public class ArticleRepository {
    private final JdbcTemplate jdbc;
    public ArticleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static final String SELECT = """
            SELECT a.*, c.name AS category FROM articles a
            LEFT JOIN article_categories c ON c.id = a.category_id
            """;
    private static final RowMapper<ArticleResponse> MAPPER = (rs, row) -> new ArticleResponse(
            rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("content"),
            rs.getObject("category_id", UUID.class), rs.getString("category"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));

    public ArticleResponse get(UUID id) {
        return jdbc.query(SELECT + " WHERE a.id = ?", MAPPER, id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public ArticlePage list(String query, UUID category, int page, int size) {
        String where = " WHERE 1=1";
        var params = new ArrayList<Object>();
        if (category != null) { where += " AND a.category_id = ?"; params.add(category); }
        boolean searching = query != null && !query.isBlank();
        if (searching) { where += " AND a.search_vector @@ websearch_to_tsquery('russian', ?)"; params.add(query); }
        long total = Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM articles a" + where, Long.class, params.toArray()));
        String order = " ORDER BY a.created_at DESC, a.id";
        if (searching) {
            order = " ORDER BY ts_rank(a.search_vector, websearch_to_tsquery('russian', ?)) DESC, a.id";
            params.add(query);
        }
        params.add(size);
        params.add((long) page * size);
        return new ArticlePage(jdbc.query(SELECT + where + order + " LIMIT ? OFFSET ?", MAPPER, params.toArray()),
                page, size, total, (total + size - 1) / size);
    }

    @Transactional
    public ArticleResponse save(UUID id, ArticleRequest request, List<String> chunks,
            List<float[]> vectors, String model, Long expectedVersion) {
        if (chunks.isEmpty() || chunks.size() != vectors.size()) throw new IllegalArgumentException("Chunks and vectors must match");
        if (request.categoryId() != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM article_categories WHERE id = ?)", Boolean.class, request.categoryId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown categoryId");
        if (expectedVersion == null) {
            jdbc.update("INSERT INTO articles(id, title, content, category_id) VALUES (?, ?, ?, ?)",
                    id, request.title(), request.content(), request.categoryId());
        } else {
            // Optimistic version prevents a slow embedding request from overwriting a newer article.
            int count = jdbc.update("""
                    UPDATE articles SET title = ?, content = ?, category_id = ?, updated_at = now(), version = version + 1
                    WHERE id = ? AND version = ?
                    """, request.title(), request.content(), request.categoryId(), id, expectedVersion);
            if (count == 0) {
                get(id); // Distinguish a deleted article from a concurrent update.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Article changed while indexing; retry the update");
            }
            jdbc.update("DELETE FROM article_embeddings WHERE article_id = ?", id);
        }
        for (int index = 0; index < chunks.size(); index++) {
            jdbc.update("""
                    INSERT INTO article_embeddings(id, article_id, chunk_index, chunk_text, embedding, model)
                    VALUES (?, ?, ?, ?, CAST(? AS vector), ?)
                    """, UUID.randomUUID(), id, index, chunks.get(index), VectorValues.literal(vectors.get(index)), model);
        }
        return get(id);
    }

    @Transactional
    public void delete(UUID id) {
        if (jdbc.update("DELETE FROM articles WHERE id = ?", id) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
    }

    public List<ScoredChunk> search(float[] vector, int topK, String model) {
        String literal = VectorValues.literal(vector);
        return jdbc.query("""
                SELECT a.id, a.title, e.chunk_text, 1 - (e.embedding <=> CAST(? AS vector)) AS score
                FROM article_embeddings e JOIN articles a ON a.id = e.article_id
                WHERE e.model = ? ORDER BY e.embedding <=> CAST(? AS vector) LIMIT ?
                """, (rs, row) -> new ScoredChunk(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getString("chunk_text"), rs.getDouble("score")), literal, model, literal, topK);
    }

    public List<CategoryResponse> categories() {
        return jdbc.query("SELECT id, name FROM article_categories ORDER BY name",
                (rs, row) -> new CategoryResponse(rs.getObject("id", UUID.class), rs.getString("name")));
    }
    public CategoryResponse createCategory(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO article_categories(id, name) VALUES (?, ?)", id, name.strip());
        return new CategoryResponse(id, name.strip());
    }
}
