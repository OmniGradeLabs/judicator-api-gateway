package com.judicator.gateway.modules.identity.internal;

import java.util.UUID;

/**
 * Lightweight DTO exposed by the identity module for internal cross-module consumption.
 *
 * <p>Only fields that other modules are allowed to read about a user are included here. This is the
 * Anti-Corruption Layer — callers MUST NOT access {@code User} entity or {@code UserRepository}
 * directly.
 */
public record UserBasicInfo(UUID id, UUID tenantId, String username, String fullName) {}
