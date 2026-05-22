package com.judicator.gateway.modules.identity.repository.jpa;

import com.judicator.gateway.modules.identity.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  @Query("SELECT u.tenant.id FROM User u WHERE u.id = :userId")
  Optional<UUID> findTenantIdByUserId(@Param("userId") UUID userId);

  Optional<User> findByUsername(String username);

  /**
   * Bulk-fetches users by a collection of IDs in a single IN-clause query. Used exclusively by
   * {@link com.judicator.gateway.modules.identity.internal.IdentityInternalApiImpl} to avoid N+1
   * when the exam module needs to enrich responses with user display names.
   */
  List<User> findAllByIdIn(Collection<UUID> ids);
}
