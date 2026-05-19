package com.judicator.gateway.modules.identity.service.auth;

import com.judicator.gateway.infrastructure.cached.redis.model.SessionAuthzCache;
import java.util.UUID;

public interface AuthorityLoader {
  SessionAuthzCache load(UUID userId);
}
