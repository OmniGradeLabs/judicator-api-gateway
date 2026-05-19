package com.judicator.gateway.common.security.config;

import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.judicator.gateway.modules.identity.repository.SessionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SessionGuardService {

  SessionRepository sessionRepository;
  SessionAuthorityCacheService sessionAuthorityCacheService;

  static final Duration ACTIVE_TTL = Duration.ofSeconds(60);

  public void ensureActive(UUID sessionId, Duration jwtTtl) {
    if (sessionId == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    Duration finalJwtTtl =
        (jwtTtl == null || jwtTtl.isNegative() || jwtTtl.isZero()) ? Duration.ofSeconds(1) : jwtTtl;

    try {
      if (sessionAuthorityCacheService.isRevoked(sessionId)) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Redis gặp sự cố ở Cửa 1 cho sessionId={}", sessionId, e);
    }

    try {
      if (sessionAuthorityCacheService.isActive(sessionId)) {
        return;
      }
    } catch (Exception e) {
      log.error("Redis gặp sự cố ở Cửa 2 cho sessionId={}", sessionId, e);
    }

    log.debug("Cache Miss hoặc Redis sập -> Tiến hành kiểm tra DB cho Session: {}", sessionId);
    boolean active = sessionRepository.isSessionActive(sessionId, LocalDateTime.now());

    if (!active) {
      try {
        sessionAuthorityCacheService.markRevoked(sessionId, finalJwtTtl);
        sessionAuthorityCacheService.clearActive(sessionId);
        sessionAuthorityCacheService.clearAuthz(sessionId);
      } catch (Exception ignore) {
      }
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    try {
      sessionAuthorityCacheService.markActive(sessionId, ACTIVE_TTL);
    } catch (Exception ignore) {
    }
  }
}
