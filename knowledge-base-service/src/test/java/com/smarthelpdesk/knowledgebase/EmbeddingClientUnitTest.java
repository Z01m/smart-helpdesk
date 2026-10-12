package com.smarthelpdesk.knowledgebase;
import com.smarthelpdesk.knowledgebase.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmbeddingClientUnitTest {
    @Test void usesSameOllamaContractAsWorker() {
        var builder = RestClient.builder().baseUrl("http://ollama");
        var server = MockRestServiceServer.bindTo(builder).build();
        String vector = "[1" + ",0".repeat(767) + "]";
        server.expect(requestTo("http://ollama/api/embed"))
                .andExpect(jsonPath("$.model").value("nomic-embed-text"))
                .andExpect(jsonPath("$.input[0]").value("Article text"))
                .andRespond(withSuccess("{\"embeddings\":[" + vector + "]}", MediaType.APPLICATION_JSON));
        var result = new EmbeddingService(builder.build(), "nomic-embed-text").embed(List.of("Article text"));
        assertThat(result.getFirst()).hasSize(768);
        server.verify();
    }
    @Test void refusesWrongModelDimensions() {
        var builder = RestClient.builder().baseUrl("http://ollama");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ollama/api/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[1,2]]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new EmbeddingService(builder.build(), "nomic-embed-text").embed(List.of("text")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
