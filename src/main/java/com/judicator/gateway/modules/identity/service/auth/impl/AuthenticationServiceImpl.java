package com.judicator.gateway.modules.identity.service.auth.impl;

import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.common.security.config.SessionAuthorityResolver;
import com.judicator.gateway.infrastructure.cached.redis.model.SessionAuthzCache;
import com.judicator.gateway.modules.identity.document.SessionDoc;
import com.judicator.gateway.modules.identity.dto.authentication.request.AuthenticationRequest;
import com.judicator.gateway.modules.identity.dto.authentication.response.UserProfileResponse;
import com.judicator.gateway.modules.identity.dto.token.request.TokenRequest;
import com.judicator.gateway.modules.identity.dto.token.response.TokenPair;
import com.judicator.gateway.modules.identity.entity.User;
import com.judicator.gateway.modules.identity.enumType.TenantStatus;
import com.judicator.gateway.modules.identity.enumType.UserStatus;
import com.judicator.gateway.modules.identity.repository.jpa.UserRepository;
import com.judicator.gateway.modules.identity.service.auth.AuthenticationService;
import com.judicator.gateway.modules.identity.service.auth.SessionService;
import com.judicator.gateway.modules.identity.service.token.TokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

  @Value("${jwt.refreshable-duration}")
  @NonFinal
  long refreshableDurationSeconds;

  @Value("${jwt.valid-duration}")
  @NonFinal
  long accessTokenTtlSeconds;

  UserRepository userRepository;
  PasswordEncoder passwordEncoder;
  TokenService tokenService;
  SessionService sessionService;
  SessionAuthorityResolver sessionAuthorityResolver;

  @Override
  @Transactional(readOnly = true)
  public TokenPair authenticate(AuthenticationRequest request) {
    // 1. Resolve user by username
    User user =
        userRepository
            .findByUsername(request.username())
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INFO));

    // 2. Verify password
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new ApiException(ErrorCode.INVALID_INFO);
    }

    // 3. Guard: user and tenant must be ACTIVE
    if (user.getStatus() != UserStatus.ACTIVE
        || user.getTenant().getStatus() != TenantStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION);
    }

    // 4. Create session in MongoDB
    SessionDoc sessionDoc = sessionService.createSession(user);

    // 5. Issue token pair
    TokenRequest tokenRequest =
        TokenRequest.builder()
            .userId(user.getId())
            .tenantId(user.getTenant().getId())
            .sessionId(sessionDoc.getId())
            .subject(user.getUsername())
            .refreshJti(sessionDoc.getRefreshJti())
            .build();

    return tokenService.generateTokenPair(tokenRequest);
  }

  @Override
  public TokenPair refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    TokenRequest extracted = tokenService.extractToken(refreshToken);
    String sessionId = extracted.getSessionId();
    String oldJti = extracted.getRefreshJti();

    // Rotate JTI in MongoDB (validates session is active + JTI matches)
    String newJti = sessionService.rotateRefreshJti(sessionId, oldJti);

    TokenRequest tokenReq =
        TokenRequest.builder()
            .userId(extracted.getUserId())
            .tenantId(extracted.getTenantId())
            .sessionId(sessionId)
            .subject(extracted.getSubject())
            .refreshJti(newJti)
            .build();

    String accessToken = tokenService.generateAccessToken(tokenReq);

    // Calculate remaining refresh TTL from session expiration
    Instant sessionExpiry = Instant.now().plusSeconds(refreshableDurationSeconds);
    Date refreshExpiry = Date.from(sessionExpiry);
    String refreshTokenNew = tokenService.generateRefreshToken(tokenReq, refreshExpiry);

    long refreshTtlRemaining = refreshableDurationSeconds;

    return TokenPair.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenNew)
        .accessTtl(accessTokenTtlSeconds)
        .refreshTtl(refreshTtlRemaining)
        .build();
  }

  @Override
  public void logout(String sessionId, UUID userId) {
    if (sessionId == null || userId == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    Duration accessTtl = Duration.ofSeconds(accessTokenTtlSeconds);
    sessionService.revoke(sessionId, userId, accessTtl);
  }

  @Override
  public void logoutAll(UUID userId) {
    sessionService.revokeAll(userId);
  }

  @Override
  public void forceLogout(UUID targetUserId) {
    sessionService.revokeAll(targetUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public UserProfileResponse getMyProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    Jwt jwt = jwtAuth.getToken();
    UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
    String sessionId = jwt.getClaimAsString("session_id");
    UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));

    SessionAuthzCache authzCache =
        sessionAuthorityResolver.resolve(sessionId, userId, jwt.getExpiresAt());

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

    return UserProfileResponse.builder()
        .id(user.getId())
        .tenantId(tenantId)
        .username(user.getUsername())
        .fullName(user.getFullName())
        .phone(user.getPhone())
        .roles(authzCache.roles())
        .permissions(authzCache.permissions())
        .build();
  }
}
