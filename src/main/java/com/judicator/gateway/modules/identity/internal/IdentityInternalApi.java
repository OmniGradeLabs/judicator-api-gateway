package com.judicator.gateway.modules.identity.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal Facade API exposed by the {@code identity} module for cross-module reads.
 *
 * <p><strong>Contract rules:</strong>
 *
 * <ul>
 *   <li>This interface is the ONLY entry-point other modules may use to query user data.
 *   <li>Callers MUST inject this interface — never {@code UserRepository} or {@code User} directly.
 *   <li>Returns {@link UserBasicInfo} only — no sensitive fields (e.g., password) are ever exposed.
 * </ul>
 */
public interface IdentityInternalApi {

  /**
   * Fetches basic profile information for a single user.
   *
   * @param userId the UUID of the user to look up
   * @return a {@link UserBasicInfo} snapshot, or {@code null} if the user does not exist
   */
  UserBasicInfo getUserById(UUID userId);

  /**
   * Bulk-fetches basic profile information for a list of user IDs in a single query.
   *
   * <p>Missing IDs are simply absent from the returned map — no exception is thrown for unknown
   * IDs. This is safe to call with an empty list (returns an empty map).
   *
   * @param userIds the list of user UUIDs to look up
   * @return a map keyed by {@code userId} containing only the IDs that were found
   */
  Map<UUID, UserBasicInfo> getUsersByIds(List<UUID> userIds);
}
