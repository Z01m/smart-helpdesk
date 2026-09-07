package com.smarthelpdesk.apigateway.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignOperatorRequest(
       @NotNull UUID operatorId
) {
}
