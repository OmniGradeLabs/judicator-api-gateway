package com.judicator.gateway;

import com.judicator.gateway.config.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: boots the full Spring context (including Flyway migrations) against live
 * Testcontainers. If the context loads without exception the entire configuration, bean wiring, and
 * DB schema are valid.
 */
class JudicatorApiGatewayApplicationTests extends BaseIntegrationTest {

  @Test
  void contextLoads() {}
}
