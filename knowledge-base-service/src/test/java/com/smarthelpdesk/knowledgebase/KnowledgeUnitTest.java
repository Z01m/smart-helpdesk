package com.smarthelpdesk.knowledgebase;
import com.smarthelpdesk.knowledgebase.dto.*;
import com.smarthelpdesk.knowledgebase.repository.ArticleRepository;
import com.smarthelpdesk.knowledgebase.service.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class KnowledgeUnitTest {
    @Test void chunkingPreservesOverlapAndTail() {
        var text = "a".repeat(2000) + "b".repeat(2100);
        var chunks = new ChunkingService().split(text);
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(2000);
        assertThat(chunks.get(1)).startsWith("a".repeat(200));
        assertThat(chunks.getLast()).isEqualTo(text.substring(3600));
    }
    @Test void rejectsEmptyNonFiniteAndZeroVectors() {
        assertThatThrownBy(() -> VectorValues.literal(new float[2])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VectorValues.literal(new float[768])).isInstanceOf(IllegalArgumentException.class);
        float[] vector = new float[768]; vector[0] = Float.NaN;
        assertThatThrownBy(() -> VectorValues.literal(vector)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void embeddingFailureDoesNotWriteArticle() {
        var repo = mock(ArticleRepository.class);
        var embeddings = mock(EmbeddingService.class);
        when(embeddings.embed(anyList())).thenThrow(new IllegalStateException("offline"));
        var service = new ArticleService(repo, new ChunkingService(), embeddings);
        assertThatThrownBy(() -> service.create(new ArticleRequest("Title", "Content", null))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repo);
    }
    @Test void workerVectorIsPassedDirectlyToSearch() {
        var repo = mock(ArticleRepository.class);
        var embeddings = mock(EmbeddingService.class);
        when(embeddings.model()).thenReturn("nomic-embed-text");
        var vector = new float[768]; vector[0] = 1;
        when(repo.search(vector, 5, "nomic-embed-text")).thenReturn(List.of());
        var result = new ArticleService(repo, new ChunkingService(), embeddings).search(new KnowledgeSearchRequest(vector, null, 5));
        assertThat(result.chunks()).isEmpty();
        verify(embeddings, never()).embed(anyList());
        verify(repo).search(vector, 5, "nomic-embed-text");
    }
    @Test void ambiguousSearchInputIsRejected() {
        var service = new ArticleService(mock(ArticleRepository.class), new ChunkingService(), mock(EmbeddingService.class));
        assertThatThrownBy(() -> service.search(new KnowledgeSearchRequest(new float[768], "query", 5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(new KnowledgeSearchRequest(null, null, 5))).isInstanceOf(IllegalArgumentException.class);
    }
}
