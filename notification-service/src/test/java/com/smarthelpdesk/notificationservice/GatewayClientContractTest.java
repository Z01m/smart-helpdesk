package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.client.GatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class GatewayClientContractTest {
 @Test void sendsInternalKeyAndReturnsMatchingOwner(){
  UUID ticket=UUID.randomUUID(),user=UUID.randomUUID();var b=RestClient.builder().baseUrl("http://gateway").defaultHeader("X-Internal-Api-Key","key");var s=MockRestServiceServer.bindTo(b).build();
  s.expect(requestTo("http://gateway/internal/v1/tickets/"+ticket+"/owner")).andExpect(header("X-Internal-Api-Key","key")).andRespond(withSuccess("{\"ticketId\":\""+ticket+"\",\"userId\":\""+user+"\"}",MediaType.APPLICATION_JSON));
  assertThat(new GatewayClient(b.build()).getTicketOwner(ticket).userId()).isEqualTo(user);s.verify();
 }
 @ParameterizedTest @ValueSource(strings={"", "{}","{\"ticketId\":\"00000000-0000-0000-0000-000000000000\",\"userId\":\"00000000-0000-0000-0000-000000000001\"}"})
 void incompleteOrMismatchedOwnerIsRejected(String body){var b=RestClient.builder().baseUrl("http://gateway");var s=MockRestServiceServer.bindTo(b).build();s.expect(anything()).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));assertThatThrownBy(()->new GatewayClient(b.build()).getTicketOwner(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);}
 @Test void networkFailureIsNotReportedAsSuccess(){var b=RestClient.builder().baseUrl("http://gateway");var s=MockRestServiceServer.bindTo(b).build();s.expect(anything()).andRespond(withServerError());assertThatThrownBy(()->new GatewayClient(b.build()).getTicketOwner(UUID.randomUUID())).isInstanceOf(org.springframework.web.client.RestClientException.class);}
}
