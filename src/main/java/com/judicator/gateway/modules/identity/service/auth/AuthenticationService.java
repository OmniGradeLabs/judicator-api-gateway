package com.judicator.gateway.modules.identity.service.auth;

import com.judicator.gateway.modules.identity.dto.authentication.request.AuthenticationRequest;
import com.judicator.gateway.modules.identity.dto.authentication.response.UserProfileResponse;
import com.judicator.gateway.modules.identity.dto.token.response.TokenPair;
import java.util.UUID;

public interface AuthenticationService {
  TokenPair authenticate(AuthenticationRequest request);

  TokenPair refresh(String refreshToken);

  void logout(String sessionId, UUID userId);

  void logoutAll(UUID userId);

  void forceLogout(UUID targetUserId);

  UserProfileResponse getMyProfile();
}
