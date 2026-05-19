package com.judicator.gateway.infrastructure.cached.redis.model;

import java.util.List;
import java.util.UUID;

public record SessionAuthzCache(
    UUID userId, UUID tenantId, List<String> roles, List<String> permissions) {}
