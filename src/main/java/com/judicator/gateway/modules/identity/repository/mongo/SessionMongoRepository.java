package com.judicator.gateway.modules.identity.repository.mongo;

import com.judicator.gateway.modules.identity.document.SessionDoc;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionMongoRepository extends MongoRepository<SessionDoc, String> {

  Optional<SessionDoc> findByIdAndRevokedAtIsNull(String id);

  List<SessionDoc> findByUserIdAndRevokedAtIsNullAndExpiredAtAfter(UUID userId, Instant now);
}
