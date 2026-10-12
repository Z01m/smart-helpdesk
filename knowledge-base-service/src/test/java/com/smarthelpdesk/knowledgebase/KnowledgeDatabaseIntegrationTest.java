package com.smarthelpdesk.knowledgebase;
import com.smarthelpdesk.knowledgebase.dto.*;
import com.smarthelpdesk.knowledgebase.repository.ArticleRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class KnowledgeDatabaseIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
    ArticleRepository repository;
    JdbcTemplate jdbc;
    TransactionTemplate transactions;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(source).load().migrate();
        jdbc = new JdbcTemplate(source);
        jdbc.execute("TRUNCATE articles CASCADE");
        repository = new ArticleRepository(jdbc);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
    }
    float[] vector(int axis) { var vector = new float[768]; vector[axis] = 1; return vector; }
    ArticleResponse create(String title, float[] vector) {
        return transactions.execute(s -> repository.save(UUID.randomUUID(), new ArticleRequest(title, "Сброс пароля", null),
                List.of("Сброс пароля"), List.of(vector), "nomic-embed-text", null));
    }
    @Test void searchRankingFullTextAndCascade() {
        var expected = create("Пароль", vector(0)); create("Доставка", vector(1));
        var matches = repository.search(vector(0), 1, "nomic-embed-text");
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().articleId()).isEqualTo(expected.id());
        assertThat(matches.getFirst().score()).isCloseTo(1.0, within(0.0001));
        assertThat(repository.list("пароля", null, 0, 10).totalElements()).isEqualTo(2);
        transactions.executeWithoutResult(s -> repository.delete(expected.id()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM article_embeddings WHERE article_id = ?", Long.class, expected.id())).isZero();
    }
    @Test void failedReindexRollsBackArticleAndExistingVectors() {
        var article = create("Original", vector(0));
        assertThatThrownBy(() -> transactions.executeWithoutResult(s -> repository.save(article.id(),
                new ArticleRequest("Changed", "changed", null), List.of("changed"), List.of(new float[2]),
                "nomic-embed-text", article.version()))).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.get(article.id()).title()).isEqualTo("Original");
        assertThat(repository.search(vector(0), 1, "nomic-embed-text").getFirst().score()).isCloseTo(1.0, within(0.0001));
    }

    @Test void staleVersionCannotOverwriteNewerArticleOrVectors() {
        var article=create("Original",vector(0));
        transactions.execute(s->repository.save(article.id(),new ArticleRequest("New","Fresh",null),List.of("Fresh"),List.of(vector(1)),"nomic-embed-text",article.version()));
        assertThatThrownBy(()->transactions.execute(s->repository.save(article.id(),new ArticleRequest("Stale","Old",null),List.of("Old"),List.of(vector(0)),"nomic-embed-text",article.version())))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        assertThat(repository.get(article.id()).title()).isEqualTo("New");
        assertThat(repository.search(vector(1),1,"nomic-embed-text").getFirst().score()).isCloseTo(1,within(.0001));
    }
    @Test void embeddingsFromDifferentModelAreNotMixed() {
        create("A",vector(0));assertThat(repository.search(vector(0),5,"another-model")).isEmpty();
    }
    @Test void unknownCategoryDoesNotLeaveHalfArticle() {
        assertThatThrownBy(()->transactions.execute(s->repository.save(UUID.randomUUID(),new ArticleRequest("A","B",UUID.randomUUID()),List.of("B"),List.of(vector(0)),"nomic-embed-text",null)))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM articles",Long.class)).isZero();
    }
    @Test void categoryFilterAndPageBoundariesAreStable() {
        var category=repository.createCategory("Category-"+UUID.randomUUID());
        transactions.execute(s->repository.save(UUID.randomUUID(),new ArticleRequest("A","Text",category.id()),List.of("Text"),List.of(vector(0)),"nomic-embed-text",null));
        create("Other",vector(1));
        assertThat(repository.list(null,category.id(),0,1).totalElements()).isEqualTo(1);
        assertThat(repository.list(null,category.id(),1,1).content()).isEmpty();
        assertThat(repository.list("'; DROP TABLE articles; --",null,0,10).totalElements()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM articles",Long.class)).isEqualTo(2);
    }
    @Test void deletedArticleCannotBeReindexedOrDeletedAgain() {
        var article=create("A",vector(0));repository.delete(article.id());
        assertThatThrownBy(()->repository.delete(article.id())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->transactions.execute(s->repository.save(article.id(),new ArticleRequest("A","B",null),List.of("B"),List.of(vector(0)),"nomic-embed-text",article.version())))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("404");
    }

}
