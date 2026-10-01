package com.smarthelpdesk.notificationservice.client;

import com.smarthelpdesk.notificationservice.dto.gateway.TicketOwnerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class GatewayClient {

    private final RestClient restClient;

    public GatewayClient(
            @Qualifier("gatewayRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public TicketOwnerResponse getTicketOwner(UUID ticketId) {

        if (ticketId == null) {
            throw new IllegalArgumentException(
                    "ticketId cannot be null"
            );
        }

        TicketOwnerResponse response =
                restClient.get()
                        .uri(
                                "/internal/v1/tickets/{ticketId}/owner",
                                ticketId
                        )
                        .retrieve()
                        .body(TicketOwnerResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "API Gateway returned null response"
            );
        }


        return response;
    }
}