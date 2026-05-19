package com.judicator.gateway.modules.identity.service.auth;

import com.judicator.gateway.modules.identity.entity.Session;
import com.judicator.gateway.modules.identity.entity.User;
import java.time.Duration;
import java.util.UUID;

public interface SessionService {
  Session createSession(User user);

  UUID rotateRefreshJti(UUID sessionId, UUID oldJti);

  void revoke(UUID sessionId, Duration accessTtl);

  void revoke(UUID sessionId, UUID userId, Duration accessTtl);

  void revokeAll(UUID userId);
}
