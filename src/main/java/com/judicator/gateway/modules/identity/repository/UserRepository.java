package com.judicator.gateway.modules.identity.repository;

import com.judicator.gateway.modules.identity.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  @Query("SELECT u.tenant.id FROM User u WHERE u.id = :userId")
  Optional<UUID> findTenantIdByUserId(@Param("userId") UUID userId);

  Optional<User> findByUsername(String username);
}
