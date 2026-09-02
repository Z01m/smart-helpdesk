package com.smarthelpdesk.apigateway.dto.response;

import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
import com.smarthelpdesk.apigateway.entity.enums.Role;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        CustomerTier customerTier
) {
}
