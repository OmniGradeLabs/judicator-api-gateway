package com.judicator.gateway.config;

import com.amazonaws.services.s3.AmazonS3;
import com.judicator.gateway.infrastructure.storage.service.MinioStorageService;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests. Spins up PostgreSQL, MongoDB, Redis and RabbitMQ containers
 * once per test suite using Testcontainers, then injects their runtime connection strings via
 * {@link DynamicPropertySource}. All subclasses share the same containers (static) to minimise
 * startup overhead while keeping each test class fully isolated at the application-context level.
 *
 * <p>External services that are NOT covered by Testcontainers (MinIO / S3) are replaced by {@link
 * MockitoBean} to prevent the ApplicationContext from crashing when constructing real AWS SDK
 * clients on a clean CI environment.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

  // ── PostgreSQL ──────────────────────────────────────────────────────────────
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("judicator_test")
          .withUsername("test")
          .withPassword("test");

  // ── MongoDB ─────────────────────────────────────────────────────────────────
  static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

  // ── Redis ───────────────────────────────────────────────────────────────────
  @SuppressWarnings("resource")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  // ── RabbitMQ ────────────────────────────────────────────────────────────────
  static final RabbitMQContainer RABBITMQ =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

  static {
    POSTGRES.start();
    MONGO.start();
    REDIS.start();
    RABBITMQ.start();
  }

  // ── MinIO / S3 — mocked (no Testcontainer) ─────────────────────────────────
  // The AmazonS3 bean is created by MinioConfig via @Value("${app.minio.access-key}") which is
  // unresolvable in CI (no .env file). MockitoBean replaces the entire bean factory output so
  // MinioConfig never runs and the context loads cleanly.
  @SuppressWarnings("unused")
  @MockitoBean
  private AmazonS3 amazonS3;

  @SuppressWarnings("unused")
  @MockitoBean
  private MinioStorageService minioStorageService;

  // Ensure containers are alive before any @BeforeAll callbacks in subclasses
  @BeforeAll
  static void ensureContainersUp() {
    if (!POSTGRES.isRunning()) {
      POSTGRES.start();
    }
    if (!MONGO.isRunning()) {
      MONGO.start();
    }
    if (!REDIS.isRunning()) {
      REDIS.start();
    }
    if (!RABBITMQ.isRunning()) {
      RABBITMQ.start();
    }
  }

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    // MongoDB
    registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);

    // Redis
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
    registry.add("spring.data.redis.timeout", () -> "2000ms");

    // RabbitMQ
    registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
    registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);

    // JWT — test key: exactly 64 bytes decoded (512 bits) to satisfy HS512 requirement
    registry.add(
        "jwt.signer-key-base64",
        () ->
            "ZzZsx2XWUT5Ty7VtfZn+z9J2piMhpVjVWEMMHavPv0ghwv6q+hlYspQaeFyFglCt1i8oB3+vfqGYhQGqVb7LTQ==");
    registry.add("jwt.valid-duration", () -> "3600");
    registry.add("jwt.refreshable-duration", () -> "604800");

    // Cookie — disable Secure flag so MockMvc can work over HTTP
    registry.add("cookie.secure", () -> "false");
    registry.add("cookie.path", () -> "/");

    // MinIO — dummy values to prevent @Value resolution failures in any non-mocked path
    registry.add("app.minio.endpoint", () -> "http://localhost:9000");
    registry.add("app.minio.access-key", () -> "test-access-key");
    registry.add("app.minio.secret-key", () -> "test-secret-key");
    registry.add("app.minio.signing-region", () -> "minio");
    registry.add("app.minio.bucket", () -> "test-bucket");
  }
}
