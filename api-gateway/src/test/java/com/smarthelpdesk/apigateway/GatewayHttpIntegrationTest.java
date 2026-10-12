package com.smarthelpdesk.apigateway;
import com.smarthelpdesk.apigateway.support.DatabaseTestSupport;
import com.smarthelpdesk.apigateway.service.AuthService;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties="jwt.access-token-expiration=30000") @AutoConfigureMockMvc
class GatewayHttpIntegrationTest extends DatabaseTestSupport {
 @Autowired com.smarthelpdesk.apigateway.security.JwtTokenProvider jwt; @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired AuthService auth; @Autowired UserRepository users;
 String token(){String email=UUID.randomUUID()+"@example.test";auth.register(email,"password-123","Test");return auth.login(email,"password-123").accessToken();}
 @Test void registrationLoginRefreshLogoutOverHttp() throws Exception {
  String email=UUID.randomUUID()+"@example.test";
  mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\"password-123\",\"fullName\":\"Test\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"));
  String body=mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\"password-123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.expiresIn").value(30000)).andReturn().getResponse().getContentAsString();
  String refresh=mapper.readTree(body).get("refreshToken").asText();
  String rotated=mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content("{\"refreshToken\":\""+refresh+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content("{\"refreshToken\":\""+refresh+"\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content("{\"refreshToken\":\""+mapper.readTree(rotated).get("refreshToken").asText()+"\"}")).andExpect(status().isNoContent());
 }
 @Test void ticketCrudRespectsOwnerAndRejectsCustomerStatusChanges() throws Exception {
  String owner=token(),other=token();String body=mvc.perform(post("/api/v1/tickets").header("Authorization","Bearer "+owner).contentType("application/json").content("{\"message\":\"Test ticket\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  String id=mapper.readTree(body).get("id").asText();
  mvc.perform(get("/api/v1/tickets/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Test ticket"));
  mvc.perform(get("/api/v1/tickets/"+id).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/tickets").header("Authorization","Bearer "+other)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
  mvc.perform(patch("/api/v1/tickets/"+id+"/status").header("Authorization","Bearer "+owner).contentType("application/json").content("{\"status\":\"CLOSED\"}")).andExpect(status().isForbidden());
 }
 @Test void invalidJsonAndInvalidIdentifierAreClientErrors() throws Exception {
  String token=token();
  mvc.perform(post("/api/v1/tickets").header("Authorization","Bearer "+token).contentType("application/json").content("not-json")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/tickets/not-a-uuid").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/tickets").header("Authorization","Bearer "+token).contentType("application/json").content("{\"message\":\" \"}")).andExpect(status().isBadRequest());
 }
 @Test void jwtForDeletedAccountIsUnauthorizedInsteadOfServerError() throws Exception {
  String email=UUID.randomUUID()+"@example.test";auth.register(email,"password-123","Test");var account=users.findByEmail(email).orElseThrow();String token=jwt.generateAccessToken(account.getId(),email,"CUSTOMER");
  users.delete(users.findByEmail(email).orElseThrow());
  mvc.perform(get("/api/v1/tickets").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }

 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Test void operatorCanAssignAndChangeStatusWithHistoryAndOutbox() throws Exception {
  String owner=token();String email=UUID.randomUUID()+"@example.test";var operator=auth.register(email,"password-123","Operator");
  jdbc.update("UPDATE users SET role='OPERATOR' WHERE id=?",operator.id());String operatorToken=auth.login(email,"password-123").accessToken();
  String body=mvc.perform(post("/api/v1/tickets").header("Authorization","Bearer "+owner).header("X-Correlation-Id","x".repeat(200)).contentType("application/json").content("{\"message\":\"Test\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  String id=mapper.readTree(body).get("id").asText();
  mvc.perform(patch("/api/v1/tickets/"+id+"/assign").header("Authorization","Bearer "+operatorToken).contentType("application/json").content("{\"operatorId\":\""+operator.id()+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.operatorId").value(operator.id().toString()));
  mvc.perform(patch("/api/v1/tickets/"+id+"/status").header("Authorization","Bearer "+operatorToken).contentType("application/json").content("{\"status\":\"PROCESSING\"}")).andExpect(status().isOk());
  org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_status_history WHERE ticket_id=?",Long.class,UUID.fromString(id))).isEqualTo(1);
  org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE aggregate_id=? AND event_type='TICKET_STATUS_CHANGED'",Long.class,UUID.fromString(id))).isEqualTo(1);
 }
 @Test void oldJwtCannotAccessRecreatedAccountWithSameEmail() throws Exception {
  String email=UUID.randomUUID()+"@example.test";var old=auth.register(email,"password-123","Old");String token=jwt.generateAccessToken(old.id(),email,"CUSTOMER");users.deleteById(old.id());auth.register(email,"different-password","New");
  mvc.perform(get("/api/v1/tickets").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }
}
