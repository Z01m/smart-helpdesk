package com.smarthelpdesk.notificationservice.security;
import java.time.Instant;
public record AccessTokenDetails(Instant expiresAt, String email) {}
