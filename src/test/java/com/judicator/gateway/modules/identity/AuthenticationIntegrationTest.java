package com.judicator.gateway.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.judicator.gateway.config.BaseIntegrationTest;
import com.judicator.gateway.infrastructure.cached.redis.service.RateLimitService;
import com.judicator.gateway.modules.identity.document.SessionDoc;
import com.judicator.gateway.modules.identity.repository.mongo.SessionMongoRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full integration test for the Authentication flow. Covers: Login → Me → Refresh → Logout →
 * LogoutAll → ForceLogout. Runs against real Testcontainers (PostgreSQL, MongoDB, Redis) so every
 * layer — Controller, Security Filter, Service, Repository — is exercised, pushing Identity-module
 * coverage well above 70 %.
 *
 * <p>RateLimitService is mocked to prevent 429 errors caused by repeated logins in the same test
 * run (all tests share the same Redis and would quickly exhaust the 5-per-60s limit on the login
 * endpoint).
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest extends BaseIntegrationTest {

  // ── Constants ───────────────────────────────────────────────────────────────
  private static final String LOGIN_URL = "/auth/login";
  private static final String REFRESH_URL = "/auth/refresh";
  private static final String ME_URL = "/auth/me";
  private static final String LOGOUT_URL = "/auth/logout";
  private static final String LOGOUT_ALL_URL = "/auth/logout-all";
  private static final String FORCE_LOGOUT_URL = "/auth/admin/users/{userId}/force-logout";

  private static final String VALID_USERNAME = "system.admin@judicator.local";
  private static final String VALID_PASSWORD = "Password@123";

  // ErrorCode numeric codes (must match ErrorCode enum)
  private static final int CODE_SUCCESS = 1000;
  private static final int CODE_INVALID_INFO = 4003;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private SessionMongoRepository sessionMongoRepository;

  /**
   * Mock out the RateLimitService so that every login call is allowed regardless of how many times
   * we call it in the test suite. Without this, repeated logins from the same IP/username would
   * trigger a 429 after the first 5 requests within 60 seconds.
   */
  @MockitoBean private RateLimitService rateLimitService;

  @BeforeEach
  void setup() {
    // Clean MongoDB between tests — each test starts from an empty session collection
    sessionMongoRepository.deleteAll();
    // Allow every rate-limit check in tests
    when(rateLimitService.allowRequest(anyString(), anyLong(), anyDouble(), anyLong()))
        .thenReturn(true);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // POST /auth/login
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @Order(1)
  @DisplayName(
      "Login thành công → trả về accessToken, set cookie refresh_token, lưu SessionDoc vào MongoDB")
  void login_withValidCredentials_returnsTokensAndPersistsSession() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(VALID_USERNAME, VALID_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(CODE_SUCCESS))
            .andExpect(jsonPath("$.result.authenticated").value(true))
            .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
            .andExpect(cookie().exists("refresh_token"))
            .andReturn();

    // Parse accessToken for downstream assertions
    String body = result.getResponse().getContentAsString();
    String at = objectMapper.readTree(body).at("/result/accessToken").asText();
    assertThat(at).isNotBlank();

    // Verify MongoDB has exactly 1 session document
    assertThat(sessionMongoRepository.count()).isEqualTo(1);
    SessionDoc doc = sessionMongoRepository.findAll().getFirst();
    assertThat(doc.getId()).isNotBlank();
    assertThat(doc.getRevokedAt()).isNull();
    assertThat(doc.getExpiredAt()).isAfter(Instant.now());
    assertThat(doc.getRefreshJti()).isNotBlank();
  }

  @Test
  @Order(2)
  @DisplayName("Login thất bại — sai password → 400 code=4003")
  void login_withWrongPassword_returns400() throws Exception {
    mockMvc
        .perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(VALID_USERNAME, "WrongPassword!")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(CODE_INVALID_INFO));

    // MongoDB phải không có session nào được tạo
    assertThat(sessionMongoRepository.count()).isZero();
  }

  @Test
  @Order(3)
  @DisplayName("Login thất bại — username không tồn tại → 400 code=4003")
  void login_withUnknownUsername_returns400() throws Exception {
    mockMvc
        .perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("ghost@nobody.com", "irrelevant")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(CODE_INVALID_INFO));
  }

  @Test
  @Order(4)
  @DisplayName("Login thất bại — request body thiếu username → 400 validation error")
  void login_withMissingUsername_returns400() throws Exception {
    String body = objectMapper.writeValueAsString(Map.of("password", "Password@123"));
    mockMvc
        .perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GET /auth/me
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @Order(5)
  @DisplayName("GET /auth/me thành công → trả về profile, roles, permissions")
  void getMyProfile_withValidToken_returnsProfile() throws Exception {
    String at = doLogin();

    mockMvc
        .perform(get(ME_URL).header("Authorization", "Bearer " + at))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.username").value(VALID_USERNAME))
        .andExpect(jsonPath("$.result.roles").isArray())
        .andExpect(jsonPath("$.result.roles[0]").value("SYSTEM_ADMIN"));
  }

  @Test
  @Order(6)
  @DisplayName("GET /auth/me không có token → 401")
  void getMyProfile_withoutToken_returns401() throws Exception {
    mockMvc.perform(get(ME_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  @Order(7)
  @DisplayName("GET /auth/me token bị sửa (chữ ký sai) → 401")
  void getMyProfile_withTamperedToken_returns401() throws Exception {
    String at = doLogin();
    String tampered = at.substring(0, at.length() - 4) + "XXXX";

    mockMvc
        .perform(get(ME_URL).header("Authorization", "Bearer " + tampered))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(8)
  @DisplayName("GET /auth/me sau khi session bị revoke thủ công trong MongoDB → 401")
  void getMyProfile_afterSessionRevoked_returns401() throws Exception {
    String at = doLogin();

    // Revoke session directly in MongoDB (simulates admin revocation or external revocation)
    SessionDoc doc = sessionMongoRepository.findAll().getFirst();
    doc.setRevokedAt(Instant.now());
    sessionMongoRepository.save(doc);

    mockMvc
        .perform(get(ME_URL).header("Authorization", "Bearer " + at))
        .andExpect(status().isUnauthorized());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // POST /auth/refresh
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @Order(9)
  @DisplayName("Refresh thành công → trả về access token mới, refresh_jti được xoay trong MongoDB")
  void refresh_withValidCookie_returnsNewTokens() throws Exception {
    MvcResult loginResult = performLogin();
    String rt = loginResult.getResponse().getCookie("refresh_token").getValue();
    String oldJti = sessionMongoRepository.findAll().getFirst().getRefreshJti();

    MvcResult refreshResult =
        mockMvc
            .perform(post(REFRESH_URL).cookie(new Cookie("refresh_token", rt)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
            .andExpect(cookie().exists("refresh_token"))
            .andReturn();

    // Verify JTI was rotated in MongoDB
    String newJti = sessionMongoRepository.findAll().getFirst().getRefreshJti();
    assertThat(newJti).isNotEqualTo(oldJti);

    // Verify new refresh token is different
    String newCookieValue = refreshResult.getResponse().getCookie("refresh_token").getValue();
    assertThat(newCookieValue).isNotEqualTo(rt);
  }

  @Test
  @Order(10)
  @DisplayName("Refresh thất bại — không có cookie và không có header → 401")
  void refresh_withNoToken_returns401() throws Exception {
    mockMvc.perform(post(REFRESH_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  @Order(11)
  @DisplayName("Refresh thất bại — dùng lại refresh token cũ (replay attack) → 401")
  void refresh_withReplayedToken_returns401() throws Exception {
    MvcResult loginResult = performLogin();
    String rt = loginResult.getResponse().getCookie("refresh_token").getValue();

    // First refresh — rotates JTI in MongoDB
    mockMvc
        .perform(post(REFRESH_URL).cookie(new Cookie("refresh_token", rt)))
        .andExpect(status().isOk());

    // Second refresh with same old token → JTI mismatch → 401
    mockMvc
        .perform(post(REFRESH_URL).cookie(new Cookie("refresh_token", rt)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(12)
  @DisplayName("Refresh hỗ trợ header X-Refresh-Token thay cho cookie → 200")
  void refresh_withHeaderToken_returns200() throws Exception {
    MvcResult loginResult = performLogin();
    String rt = loginResult.getResponse().getCookie("refresh_token").getValue();

    mockMvc
        .perform(post(REFRESH_URL).header("X-Refresh-Token", rt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.accessToken").isNotEmpty());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // POST /auth/logout
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @Order(13)
  @DisplayName("Logout thành công → session bị revoke trong MongoDB, cookie bị xóa (maxAge=0)")
  void logout_withValidToken_revokesSessionAndClearsCookie() throws Exception {
    String at = doLogin();

    mockMvc
        .perform(post(LOGOUT_URL).header("Authorization", "Bearer " + at))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("refresh_token", 0));

    // Session phải có revokedAt != null
    SessionDoc doc = sessionMongoRepository.findAll().getFirst();
    assertThat(doc.getRevokedAt()).isNotNull();

    // Token cũ không còn dùng được sau khi logout
    mockMvc
        .perform(get(ME_URL).header("Authorization", "Bearer " + at))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(14)
  @DisplayName("Logout thất bại — không có token → 401")
  void logout_withoutToken_returns401() throws Exception {
    mockMvc.perform(post(LOGOUT_URL)).andExpect(status().isUnauthorized());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // POST /auth/logout-all
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @Order(15)
  @DisplayName("LogoutAll → tất cả session của user bị revoke trong MongoDB")
  void logoutAll_revokesAllSessionsForUser() throws Exception {
    // Tạo 2 session cho cùng 1 user (giả lập đăng nhập từ 2 thiết bị)
    String at1 = doLogin();
    String at2 = doLogin();

    assertThat(sessionMongoRepository.count()).isEqualTo(2);

    // Logout all bằng token của device 1
    mockMvc
        .perform(post(LOGOUT_ALL_URL).header("Authorization", "Bearer " + at1))
        .andExpect(status().isOk());

    // Cả 2 session phải có revokedAt
    long revokedCount =
        sessionMongoRepository.findAll().stream().filter(d -> d.getRevokedAt() != null).count();
    assertThat(revokedCount).isEqualTo(2);

    // Token của device 2 cũng không còn dùng được
    mockMvc
        .perform(get(ME_URL).header("Authorization", "Bearer " + at2))
        .andExpect(status().isUnauthorized());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // POST /auth/admin/users/{userId}/force-logout
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @Order(16)
  @DisplayName("ForceLogout (SYSTEM_ADMIN) → tất cả session của target user bị revoke")
  void forceLogout_asSystemAdmin_revokesTargetUserSessions() throws Exception {
    String at = doLogin();
    SessionDoc doc = sessionMongoRepository.findAll().getFirst();
    String userId = doc.getUserId().toString();

    mockMvc
        .perform(post(FORCE_LOGOUT_URL, userId).header("Authorization", "Bearer " + at))
        .andExpect(status().isOk());

    // Session phải bị revoke
    SessionDoc updated = sessionMongoRepository.findAll().getFirst();
    assertThat(updated.getRevokedAt()).isNotNull();
  }

  @Test
  @Order(17)
  @DisplayName("ForceLogout không có token → 401 (unauthenticated)")
  void forceLogout_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(post(FORCE_LOGOUT_URL, "00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isUnauthorized());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helper Methods
  // ═══════════════════════════════════════════════════════════════════════════

  private MvcResult performLogin() throws Exception {
    return mockMvc
        .perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(VALID_USERNAME, VALID_PASSWORD)))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String doLogin() throws Exception {
    MvcResult result = performLogin();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .at("/result/accessToken")
        .asText();
  }

  private String loginBody(String username, String password) throws Exception {
    return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
  }
}
