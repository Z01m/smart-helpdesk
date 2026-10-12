package com.smarthelpdesk.apigateway;
import com.smarthelpdesk.apigateway.util.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.slf4j.MDC;
import static org.assertj.core.api.Assertions.*;
class CorrelationIdTest {
 @ParameterizedTest @ValueSource(strings={"valid-id_123", "", "has spaces", "x\nforged-log", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
 void keepsOnlyBoundedSafeHeaderAndAlwaysClearsThreadContext(String header) throws Exception {
  var request=new MockHttpServletRequest();request.addHeader("X-Correlation-Id",header);var response=new MockHttpServletResponse();
  new CorrelationIdFilter().doFilter(request,response,(req,res)->assertThat(MDC.get("correlationId")).matches("[A-Za-z0-9._-]{1,64}"));
  assertThat(MDC.get("correlationId")).isNull();assertThat(response.getHeader("X-Correlation-Id")).matches("[A-Za-z0-9._-]{1,64}");
  if(header.equals("valid-id_123"))assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(header);
 }
 @Test void clearsContextEvenWhenRequestFails(){var request=new MockHttpServletRequest();assertThatThrownBy(()->new CorrelationIdFilter().doFilter(request,new MockHttpServletResponse(),(q,s)->{throw new java.io.IOException("offline");})).isInstanceOf(java.io.IOException.class);assertThat(MDC.get("correlationId")).isNull();}
}
