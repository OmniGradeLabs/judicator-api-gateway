package com.judicator.gateway.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a Tenant is suspended in the identity module.
 *
 * <p>Placed in {@code common.events} so it is visible to ALL modules without creating a direct
 * dependency from any module back into {@code identity}. Consumers (e.g., the {@code exam} module)
 * listen via {@code @EventListener} or {@code @TransactionalEventListener}.
 *
 * @param tenantId the UUID of the suspended tenant
 * @param suspendedAt the exact instant the suspension was committed
 */
public record TenantSuspendedEvent(UUID tenantId, Instant suspendedAt) {}
