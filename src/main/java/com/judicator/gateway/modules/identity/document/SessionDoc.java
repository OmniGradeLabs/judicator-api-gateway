package com.judicator.gateway.modules.identity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDoc {

  @Id private String id;

  @Indexed
  @Field("user_id")
  private UUID userId;

  @Indexed
  @Field("tenant_id")
  private UUID tenantId;

  @Field("refresh_jti")
  private String refreshJti;

  @Field("revoked_at")
  private Instant revokedAt;

  @Indexed
  @Field("expired_at")
  private Instant expiredAt;

  @CreatedDate
  @Field("created_at")
  private Instant createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private Instant updatedAt;
}
