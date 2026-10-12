package com.smarthelpdesk.knowledgebase;
import com.smarthelpdesk.knowledgebase.config.SecurityConfig;
import com.smarthelpdesk.knowledgebase.controller.*;
import com.smarthelpdesk.knowledgebase.security.AccessTokenVerifier;
import com.smarthelpdesk.knowledgebase.service.ArticleService;
import com.smarthelpdesk.knowledgebase.dto.KnowledgeSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ArticleController.class, InternalSearchController.class}, properties = {
        "internal.api-key=test-internal", "jwt.secret=01234567890123456789012345678901234567890123456789"})
@Import({SecurityConfig.class, AccessTokenVerifier.class})
class KnowledgeSecurityWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean ArticleService service;
    @Test void internalSearchRequiresCorrectKey() throws Exception {
        mvc.perform(post("/internal/v1/knowledge/search").contentType("application/json")
                .content("{\"query\":\"help\",\"topK\":5}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/v1/knowledge/search").header("X-Internal-Api-Key", "wrong")
                .contentType("application/json").content("{\"query\":\"help\",\"topK\":5}")).andExpect(status().isUnauthorized());
    }
    @Test void correctKeyAllowsWorkerContract() throws Exception {
        when(service.search(any())).thenReturn(new KnowledgeSearchResponse(List.of()));
        mvc.perform(post("/internal/v1/knowledge/search").header("X-Internal-Api-Key", "test-internal")
                .contentType("application/json").content("{\"embedding\":[1,0],\"topK\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.chunks").isArray());
    }
    @Test void customerCannotWriteArticles() throws Exception {
        mvc.perform(post("/api/v1/articles").with(user("customer").roles("CUSTOMER"))
                .contentType("application/json").content("{\"title\":\"t\",\"content\":\"text\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void adminInputIsValidated() throws Exception {
        mvc.perform(post("/api/v1/articles").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content("{\"title\":\"\",\"content\":\"text\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
