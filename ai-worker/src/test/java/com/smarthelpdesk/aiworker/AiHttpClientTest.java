package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.ai.OllamaClient;
import com.smarthelpdesk.aiworker.config.AiClientConfig;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import com.smarthelpdesk.aiworker.client.KnowledgeBaseClient;
import com.smarthelpdesk.aiworker.dto.knowledge.KnowledgeSearchRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;
class AiHttpClientTest {
 RestClient.Builder builder;MockRestServiceServer server;OllamaClient client;
 final PromptRequest prompt=new PromptRequest("system","question",.1,.9,"model");
 @BeforeEach void setup(){builder=RestClient.builder().baseUrl("http://test");server=MockRestServiceServer.bindTo(builder).build();var config=new AiClientConfig();config.setEmbeddingModel("nomic-embed-text");client=new OllamaClient(builder.build(),config);}
 @Test void chatContractIncludesRolesAndDisablesStreaming(){
  server.expect(requestTo("http://test/api/chat")).andExpect(jsonPath("$.stream").value(false)).andExpect(jsonPath("$.messages[0].role").value("system")).andExpect(jsonPath("$.messages[1].content").value("question"))
   .andRespond(withSuccess("{\"model\":\"model\",\"message\":{\"role\":\"assistant\",\"content\":\"answer\"},\"done\":true,\"prompt_eval_count\":10,\"eval_count\":5}",MediaType.APPLICATION_JSON));
  var result=client.chatCompletion(prompt);assertThat(result.rawText()).isEqualTo("answer");assertThat(result.usage().get("completionTokens")).isEqualTo(5L);server.verify();
 }
 @ParameterizedTest @ValueSource(strings={"", "{}", "{\"message\":{\"content\":\" \"},\"done\":true}","{\"message\":{\"content\":\"partial\"},\"done\":false}"})
 void incompleteChatIsNotAccepted(String body){server.expect(requestTo("http://test/api/chat")).andRespond(withSuccess(complete(body),MediaType.APPLICATION_JSON));assertThatThrownBy(()->client.chatCompletion(prompt)).isInstanceOfAny(IllegalStateException.class,org.springframework.web.client.RestClientException.class);}
 @Test void embeddingUsesConfiguredModel(){server.expect(requestTo("http://test/api/embed")).andExpect(jsonPath("$.model").value("nomic-embed-text")).andRespond(withSuccess(complete("{\"embeddings\":[[1,0,0]]}"),MediaType.APPLICATION_JSON));assertThat(client.embed("text")).containsExactly(1,0,0);}
 @ParameterizedTest @ValueSource(strings={"{}","{\"embeddings\":[]}","{\"embeddings\":[[]]}","{\"embeddings\":[[null]]}","{\"embeddings\":[[\"NaN\"]]}"})
 void rejectsInvalidEmbedding(String body){server.expect(requestTo("http://test/api/embed")).andRespond(withSuccess(complete(body),MediaType.APPLICATION_JSON));assertThatThrownBy(()->client.embed("text")).isInstanceOfAny(IllegalStateException.class,org.springframework.web.client.RestClientException.class);}
 @Test void upstreamFailurePropagatesForRetry(){server.expect(requestTo("http://test/api/chat")).andRespond(withServerError());assertThatThrownBy(()->client.chatCompletion(prompt)).isInstanceOf(org.springframework.web.client.RestClientException.class);}
 @Test void knowledgeSearchSendsProtectedContract(){
  var b=RestClient.builder().baseUrl("http://kb").defaultHeader("X-Internal-Api-Key","test-key");var s=MockRestServiceServer.bindTo(b).build();
  s.expect(requestTo("http://kb/internal/v1/knowledge/search")).andExpect(header("X-Internal-Api-Key","test-key")).andExpect(jsonPath("$.topK").value(3)).andRespond(withSuccess("{\"chunks\":[]}",MediaType.APPLICATION_JSON));
  assertThat(new KnowledgeBaseClient(b.build()).search(new KnowledgeSearchRequest(new float[]{1,0},3)).chunks()).isEmpty();s.verify();
 }
 String complete(String body){
  if(body.isEmpty())return body;
  body=body.replace("\"content\":", "\"role\":\"assistant\",\"content\":");
  String metadata=body.contains("embeddings") ? "\"total_duration\":0,\"load_duration\":0," : "\"eval_count\":0,";
  return "{\"model\":\"model\",\"prompt_eval_count\":0,"+metadata+body.substring(1);
 }
}
