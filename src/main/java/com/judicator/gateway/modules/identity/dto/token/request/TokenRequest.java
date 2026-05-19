package com.judicator.gateway.modules.identity.dto.token.request;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRequest {
  private UUID userId;
  private UUID tenantId;
  private String sessionId;
  private String subject;
  private String refreshJti;
}
