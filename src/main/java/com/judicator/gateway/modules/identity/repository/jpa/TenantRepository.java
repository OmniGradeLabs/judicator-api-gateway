package com.judicator.gateway.modules.identity.repository.jpa;

import com.judicator.gateway.modules.identity.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
  boolean existsBySlug(String slug);

  Optional<Tenant> findBySlug(String slug);
}
