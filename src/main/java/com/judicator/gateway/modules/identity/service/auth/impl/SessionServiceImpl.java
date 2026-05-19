package com.judicator.gateway.modules.identity.service.auth.impl;

import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.judicator.gateway.infrastructure.persistence.UuidV7;
import com.judicator.gateway.modules.identity.document.SessionDoc;
import com.judicator.gateway.modules.identity.entity.User;
import com.judicator.gateway.modules.identity.repository.mongo.SessionMongoRepository;
import com.judicator.gateway.modules.identity.service.auth.SessionService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class SessionServiceImpl implements SessionService {

  @Value("${jwt.refreshable-duration}")
  long refreshableDurationSeconds;

  final SessionMongoRepository sessionMongoRepository;
  final SessionAuthorityCacheService sessionAuthorityCacheService;

  @Override
  public SessionDoc createSession(User user) {
    SessionDoc doc =
        SessionDoc.builder()
            .userId(user.getId())
            .tenantId(user.getTenant().getId())
            .refreshJti(UuidV7.random().toString())
            .revokedAt(null)
            .expiredAt(Instant.now().plusSeconds(refreshableDurationSeconds))
            .build();
    return sessionMongoRepository.save(doc);
  }

  @Override
  public String rotateRefreshJti(String sessionId, String oldJti) {
    SessionDoc doc =
        sessionMongoRepository
            .findByIdAndRevokedAtIsNull(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

    if (!oldJti.equals(doc.getRefreshJti())) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    if (doc.getExpiredAt() == null || doc.getExpiredAt().isBefore(Instant.now())) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    String newJti = UuidV7.random().toString();
    doc.setRefreshJti(newJti);
    sessionMongoRepository.save(doc);
    return newJti;
  }

  @Override
  public void revoke(String sessionId, Duration accessTtl) {
    if (sessionId == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    revokeSession(sessionId, null, accessTtl);
  }

  @Override
  public void revoke(String sessionId, UUID userId, Duration accessTtl) {
    if (sessionId == null || userId == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    revokeSession(sessionId, userId, accessTtl);
  }

  private void revokeSession(String sessionId, UUID userId, Duration accessTtl) {
    if (accessTtl == null || accessTtl.isZero() || accessTtl.isNegative()) {
      accessTtl = Duration.ofSeconds(1);
    }

    SessionDoc doc =
        sessionMongoRepository
            .findByIdAndRevokedAtIsNull(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

    // Ràng buộc đúng chủ session để tránh revoke nhầm user
    if (userId != null && !userId.equals(doc.getUserId())) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    doc.setRevokedAt(Instant.now());
    sessionMongoRepository.save(doc);

    try {
      sessionAuthorityCacheService.markRevoked(sessionId, accessTtl);
      sessionAuthorityCacheService.clearAuthz(sessionId);
      sessionAuthorityCacheService.clearActive(sessionId);
    } catch (Exception e) {
      log.warn("Revoke redis best-effort failed sessionId={}", sessionId, e);
    }
  }

  @Override
  public void revokeAll(UUID userId) {
    Instant now = Instant.now();
    List<SessionDoc> activeSessions =
        sessionMongoRepository.findByUserIdAndRevokedAtIsNullAndExpiredAtAfter(userId, now);

    Duration accessTtl = Duration.ofMinutes(15);
    for (SessionDoc doc : activeSessions) {
      doc.setRevokedAt(now);
      sessionMongoRepository.save(doc);

      try {
        sessionAuthorityCacheService.markRevoked(doc.getId(), accessTtl);
        sessionAuthorityCacheService.clearAuthz(doc.getId());
        sessionAuthorityCacheService.clearActive(doc.getId());
      } catch (Exception e) {
        log.warn("RevokeAll redis best-effort failed sessionId={}", doc.getId(), e);
      }
    }
  }
}
