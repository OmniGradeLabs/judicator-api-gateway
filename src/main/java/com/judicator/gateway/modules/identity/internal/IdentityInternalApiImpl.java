package com.judicator.gateway.modules.identity.internal;

import com.judicator.gateway.modules.identity.entity.User;
import com.judicator.gateway.modules.identity.repository.jpa.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concrete implementation of the {@link IdentityInternalApi} facade.
 *
 * <p>This class is the only point inside the {@code identity} module that bridges internal
 * persistence ({@link UserRepository}, {@link User}) to a decoupled, module-safe representation
 * ({@link UserBasicInfo}).
 *
 * <p><strong>Visibility note:</strong> intentionally package-private in logical terms — other
 * modules MUST inject the {@link IdentityInternalApi} interface, never this concrete class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IdentityInternalApiImpl implements IdentityInternalApi {

  UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserBasicInfo getUserById(UUID userId) {
    log.debug("[IdentityFacade] Fetching single user profile for ID: {}", userId);

    return userRepository.findById(userId).map(this::toUserBasicInfo).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, UserBasicInfo> getUsersByIds(List<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }

    log.debug("[IdentityFacade] Bulk-fetching user profiles for {} IDs", userIds.size());

    return userRepository.findAllByIdIn(userIds).stream()
        .map(this::toUserBasicInfo)
        .collect(Collectors.toMap(UserBasicInfo::id, Function.identity()));
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private UserBasicInfo toUserBasicInfo(User user) {
    return new UserBasicInfo(
        user.getId(),
        // tenantId is a raw UUID field on User (not a @ManyToOne), already fetched
        user.getTenant() != null ? user.getTenant().getId() : null,
        user.getUsername(),
        user.getFullName());
  }
}
