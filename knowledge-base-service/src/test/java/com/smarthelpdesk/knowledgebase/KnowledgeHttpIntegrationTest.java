package com.smarthelpdesk.knowledgebase;
import com.smarthelpdesk.knowledgebase.service.EmbeddingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class KnowledgeHttpIntegrationTest {
 static final PostgreSQLContainer<?> db=new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));static{db.start();}
 @DynamicPropertySource static void config(DynamicPropertyRegistry r){r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);}
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;@Value("${jwt.secret}") String secret;@Value("${internal.api-key}") String internal;
 @MockitoBean EmbeddingService embeddings;
 @BeforeEach void setup(){jdbc.execute("TRUNCATE articles CASCADE");float[] v=new float[768];v[0]=1;when(embeddings.embed(any())).thenAnswer(i->((List<?>)i.getArgument(0)).stream().map(t->v).toList());when(embeddings.model()).thenReturn("nomic-embed-text");}
 String token(String role){return io.jsonwebtoken.Jwts.builder().subject(UUID.randomUUID().toString()).claim("role",role).claim("type","access").expiration(new Date(System.currentTimeMillis()+60000)).signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();}
 String article(String admin) throws Exception {return mapper.readTree(mvc.perform(post("/api/v1/articles").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"title\":\"Reset password\",\"content\":\"Reset using email\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();}
 @Test void adminCanCreateUpdateReindexSearchAndDelete() throws Exception {
  String admin=token("ADMIN"),id=article(admin);
  mvc.perform(get("/api/v1/articles/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
  mvc.perform(put("/api/v1/articles/"+id).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"title\":\"New title\",\"content\":\"New content\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(post("/api/v1/articles/"+id+"/reindex").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
  mvc.perform(post("/internal/v1/knowledge/search").header("X-Internal-Api-Key",internal).contentType("application/json").content("{\"query\":\"New content\",\"topK\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.chunks[0].articleId").value(id));
  mvc.perform(delete("/api/v1/articles/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/articles/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isNotFound());
 }
 @Test void authenticatedReaderCannotModifyOrUseInternalKeyAsJwt() throws Exception {
  String id=article(token("ADMIN")),customer=token("CUSTOMER");
  mvc.perform(get("/api/v1/articles/"+id).header("Authorization","Bearer "+customer)).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/articles/"+id).header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/articles").header("X-Internal-Api-Key",internal)).andExpect(status().isUnauthorized());
  mvc.perform(post("/internal/v1/knowledge/search").header("Authorization","Bearer "+token("ADMIN")).contentType("application/json").content("{\"query\":\"x\",\"topK\":1}")).andExpect(status().isUnauthorized());
 }
 @Test void upstreamFailureDoesNotReplaceExistingArticle() throws Exception {
  String admin=token("ADMIN"),id=article(admin);doThrow(new org.springframework.web.client.ResourceAccessException("offline")).when(embeddings).embed(any());
  mvc.perform(put("/api/v1/articles/"+id).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"title\":\"Broken update\",\"content\":\"Text\"}")).andExpect(status().isBadGateway());
  mvc.perform(get("/api/v1/articles/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Reset password"));
 }
 @Test void categoryAndSearchInputValidation() throws Exception {
  String admin=token("ADMIN");String name="Category-"+UUID.randomUUID();String json="{\"name\":\""+name+"\"}";
  mvc.perform(post("/api/v1/categories").header("Authorization","Bearer "+admin).contentType("application/json").content(json)).andExpect(status().isCreated());
  mvc.perform(post("/api/v1/categories").header("Authorization","Bearer "+admin).contentType("application/json").content(json)).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/categories").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/articles?size=101").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
  mvc.perform(post("/internal/v1/knowledge/search").header("X-Internal-Api-Key",internal).contentType("application/json").content("{\"query\":\"x\",\"embedding\":[1,0],\"topK\":1}")).andExpect(status().isBadRequest());
 }
}
