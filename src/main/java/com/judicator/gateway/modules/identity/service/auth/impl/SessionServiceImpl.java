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

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionServiceImpl implements SessionService {

  @Value("${jwt.refreshable-duration}")
  long refreshableDurationSeconds;

  // MongoDB repository — không phải JPA, không dùng @Transactional
  final SessionMongoRepository sessionMongoRepository;
  final SessionAuthorityCacheService sessionAuthorityCacheService;

  /**
   * Tạo session mới trong MongoDB và trả về {@link SessionDoc}.
   *
   * <p>MongoDB không tham gia vào JPA transaction của PostgreSQL, nên không cần
   * {@code @Transactional}. {@code refreshJti} được sinh bằng UUIDv7 dưới dạng {@code String} (khớp
   * với kiểu {@code String} trong {@link SessionDoc}).
   */
  @Override
  public SessionDoc createSession(User user) {
    SessionDoc session =
        SessionDoc.builder()
            .userId(user.getId())
            .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
            // refreshJti lưu dưới dạng String trong MongoDB, không phải UUID
            .refreshJti(UuidV7.random().toString())
            .expiredAt(Instant.now().plusSeconds(refreshableDurationSeconds))
            .revokedAt(null)
            .build();

    SessionDoc saved = sessionMongoRepository.save(session);
    log.debug(
        "[SessionService] Tạo session thành công — sessionId: {}, userId: {}",
        saved.getId(),
        user.getId());
    return saved;
  }

  /**
   * Xoay vòng (rotate) JTI của refresh token.
   *
   * <p>Cả {@code sessionId} lẫn {@code oldJti} đều là {@code String} vì MongoDB dùng {@code String}
   * cho {@code @Id} và {@code refreshJti}.
   */
  @Override
  public String rotateRefreshJti(String sessionId, String oldJti) {
    SessionDoc session =
        sessionMongoRepository
            .findByIdAndRevokedAtIsNull(sessionId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[SessionService] rotateRefreshJti — session không tồn tại hoặc đã bị revoke: {}",
                      sessionId);
                  return new ApiException(ErrorCode.UNAUTHENTICATED);
                });

    // JTI không khớp → replay attack tiềm năng
    if (!oldJti.equals(session.getRefreshJti())) {
      log.warn("[SessionService] rotateRefreshJti — JTI không khớp cho sessionId: {}", sessionId);
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    String newJti = UuidV7.random().toString();
    session.setRefreshJti(newJti);
    sessionMongoRepository.save(session);

    log.debug("[SessionService] Đã rotate JTI thành công — sessionId: {}", sessionId);
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

  @Override
  public void revokeAll(UUID userId) {
    List<SessionDoc> activeSessions =
        sessionMongoRepository.findByUserIdAndRevokedAtIsNullAndExpiredAtAfter(
            userId, Instant.now());

    if (activeSessions.isEmpty()) {
      log.debug("[SessionService] revokeAll — không có session active nào cho userId: {}", userId);
      return;
    }

    Instant now = Instant.now();
    Duration accessTtl = Duration.ofMinutes(15);

    for (SessionDoc session : activeSessions) {
      session.setRevokedAt(now);
      sessionMongoRepository.save(session);

      try {
        // sessionId trong MongoDB là String — truyền thẳng vào cache service
        sessionAuthorityCacheService.markRevoked(session.getId(), accessTtl);
        sessionAuthorityCacheService.clearAuthz(session.getId());
        sessionAuthorityCacheService.clearActive(session.getId());
      } catch (Exception e) {
        log.warn(
            "[SessionService] revokeAll — xoá Redis best-effort thất bại, sessionId: {}",
            session.getId(),
            e);
      }
    }

    log.info(
        "[SessionService] Đã revoke {} session(s) cho userId: {}", activeSessions.size(), userId);
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private void revokeSession(String sessionId, UUID expectedUserId, Duration accessTtl) {
    if (accessTtl == null || accessTtl.isZero() || accessTtl.isNegative()) {
      accessTtl = Duration.ofSeconds(1);
    }

    SessionDoc session =
        sessionMongoRepository
            .findByIdAndRevokedAtIsNull(sessionId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[SessionService] revoke — session không tồn tại hoặc đã bị revoke: {}",
                      sessionId);
                  return new ApiException(ErrorCode.UNAUTHENTICATED);
                });

    // Nếu có expectedUserId, kiểm tra session có thuộc về đúng user không
    if (expectedUserId != null && !expectedUserId.equals(session.getUserId())) {
      log.warn(
          "[SessionService] revoke — userId không khớp, sessionId: {}, expectedUserId: {}",
          sessionId,
          expectedUserId);
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    session.setRevokedAt(Instant.now());
    sessionMongoRepository.save(session);

    try {
      // sessionId là String — khớp hoàn toàn với SessionAuthorityCacheService.markRevoked(String)
      sessionAuthorityCacheService.markRevoked(sessionId, accessTtl);
      sessionAuthorityCacheService.clearAuthz(sessionId);
      sessionAuthorityCacheService.clearActive(sessionId);
    } catch (Exception e) {
      log.warn(
          "[SessionService] revoke — xoá Redis best-effort thất bại, sessionId: {}", sessionId, e);
    }
  }
}
