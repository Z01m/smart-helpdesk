package com.smarthelpdesk.apigateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "message cannot be empty")
        @Size(min = 1, max = 5000, message = "message must contain from 10 to 5000 characters")
        String message)
{
}
