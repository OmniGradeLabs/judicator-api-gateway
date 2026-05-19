package com.judicator.gateway.modules.identity.service.auth;

import com.judicator.gateway.modules.identity.document.SessionDoc;
import com.judicator.gateway.modules.identity.entity.User;
import java.time.Duration;
import java.util.UUID;

public interface SessionService {
  SessionDoc createSession(User user);

  String rotateRefreshJti(String sessionId, String oldJti);

  void revoke(String sessionId, Duration accessTtl);

  void revoke(String sessionId, UUID userId, Duration accessTtl);

  void revokeAll(UUID userId);
}
