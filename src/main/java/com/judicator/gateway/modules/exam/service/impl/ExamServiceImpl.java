package com.judicator.gateway.modules.exam.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.infrastructure.cached.redis.service.ExamCacheService;
import com.judicator.gateway.modules.exam.dto.request.CreateExamRequest;
import com.judicator.gateway.modules.exam.dto.request.UpdateExamRequest;
import com.judicator.gateway.modules.exam.dto.response.ExamResponse;
import com.judicator.gateway.modules.exam.entity.Exam;
import com.judicator.gateway.modules.exam.entity.ExamRule;
import com.judicator.gateway.modules.exam.mapper.ExamMapper;
import com.judicator.gateway.modules.exam.repository.ExamRepository;
import com.judicator.gateway.modules.exam.repository.ExamRuleRepository;
import com.judicator.gateway.modules.exam.service.ExamService;
import com.judicator.gateway.modules.identity.internal.IdentityInternalApi;
import com.judicator.gateway.modules.identity.internal.UserBasicInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ExamServiceImpl implements ExamService {

  ExamRepository examRepository;
  ExamRuleRepository examRuleRepository;
  ExamMapper examMapper;
  ExamCacheService examCacheService;
  IdentityInternalApi identityInternalApi;
  ObjectMapper objectMapper;

  @Override
  @Transactional
  public ExamResponse createExam(CreateExamRequest request) {
    CurrentPrincipal principal = currentPrincipal();
    validateRulePayload(request.rulePayload());
    String teacherName = resolveCurrentTeacherName(principal);

    Exam exam = examMapper.toEntity(request);
    exam.setTenantId(principal.tenantId());
    exam.setCreatedByTeacherId(principal.userId());
    exam.setDeleted(false);

    Exam savedExam = examRepository.save(exam);
    ExamRule rule = examMapper.toRuleEntity(request);
    rule.setExam(savedExam);
    ExamRule savedRule = examRuleRepository.save(rule);

    ExamResponse response = examMapper.toResponse(savedExam, savedRule, teacherName);
    examCacheService.saveExam(principal.tenantId(), savedExam.getId(), response);
    examCacheService.evictExamList(principal.tenantId());

    log.info(
        "[Exam] Created exam id={} tenantId={} teacherId={}",
        savedExam.getId(),
        principal.tenantId(),
        principal.userId());
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExamResponse> getAllExams() {
    CurrentPrincipal principal = currentPrincipal();

    return examCacheService
        .getExamList(principal.tenantId())
        .orElseGet(
            () -> {
              log.debug("[Exam] Cache miss for tenant exam list tenantId={}", principal.tenantId());
              List<Exam> exams =
                  examRepository.findAllByTenantIdOrderByCreatedAtDesc(principal.tenantId());
              List<ExamResponse> responses = buildResponses(exams, principal.tenantId());
              examCacheService.saveExamList(principal.tenantId(), responses);
              return responses;
            });
  }

  @Override
  @Transactional(readOnly = true)
  public ExamResponse getExamById(UUID id) {
    CurrentPrincipal principal = currentPrincipal();

    return examCacheService
        .getExam(principal.tenantId(), id)
        .orElseGet(
            () -> {
              log.debug(
                  "[Exam] Cache miss for exam detail tenantId={} examId={}",
                  principal.tenantId(),
                  id);
              Exam exam = findTenantExam(id, principal.tenantId());
              ExamRule rule = findTenantRule(id, principal.tenantId());
              String teacherName = resolveTeacherName(exam.getCreatedByTeacherId());
              ExamResponse response = examMapper.toResponse(exam, rule, teacherName);
              examCacheService.saveExam(principal.tenantId(), id, response);
              return response;
            });
  }

  @Override
  @Transactional
  public ExamResponse updateExam(UUID id, UpdateExamRequest request) {
    CurrentPrincipal principal = currentPrincipal();
    validateRulePayload(request.rulePayload());

    Exam exam = findTenantExam(id, principal.tenantId());
    examMapper.updateEntity(request, exam);
    Exam savedExam = examRepository.save(exam);

    ExamRule rule =
        examRuleRepository
            .findByExam_IdAndExam_TenantId(id, principal.tenantId())
            .orElseGet(() -> ExamRule.builder().exam(savedExam).build());
    examMapper.updateRule(request, rule);
    ExamRule savedRule = examRuleRepository.save(rule);

    String teacherName = resolveTeacherName(savedExam.getCreatedByTeacherId());
    ExamResponse response = examMapper.toResponse(savedExam, savedRule, teacherName);
    examCacheService.saveExam(principal.tenantId(), id, response);
    examCacheService.evictExamList(principal.tenantId());

    log.info("[Exam] Updated exam id={} tenantId={}", id, principal.tenantId());
    return response;
  }

  @Override
  @Transactional
  public void deleteExam(UUID id) {
    CurrentPrincipal principal = currentPrincipal();
    Exam exam = findTenantExam(id, principal.tenantId());
    exam.setDeleted(true);
    examRepository.save(exam);

    examCacheService.evictExam(principal.tenantId(), id);
    examCacheService.evictExamList(principal.tenantId());

    log.info("[Exam] Soft deleted exam id={} tenantId={}", id, principal.tenantId());
  }

  private List<ExamResponse> buildResponses(List<Exam> exams, UUID tenantId) {
    if (exams.isEmpty()) {
      return List.of();
    }

    List<UUID> examIds = exams.stream().map(Exam::getId).toList();
    Map<UUID, ExamRule> rulesByExamId =
        examRuleRepository.findAllByExamIdsAndTenantId(examIds, tenantId).stream()
            .collect(Collectors.toMap(rule -> rule.getExam().getId(), Function.identity()));

    List<UUID> teacherIds =
        exams.stream()
            .map(Exam::getCreatedByTeacherId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<UUID, UserBasicInfo> teachersById = identityInternalApi.getUsersByIds(teacherIds);

    return exams.stream()
        .map(
            exam ->
                examMapper.toResponse(
                    exam,
                    rulesByExamId.get(exam.getId()),
                    displayName(teachersById.get(exam.getCreatedByTeacherId()))))
        .toList();
  }

  private Exam findTenantExam(UUID id, UUID tenantId) {
    return examRepository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(
            () ->
                new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài thi trong tenant hiện tại"));
  }

  private ExamRule findTenantRule(UUID examId, UUID tenantId) {
    return examRuleRepository
        .findByExam_IdAndExam_TenantId(examId, tenantId)
        .orElseThrow(
            () ->
                new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy rule của bài thi"));
  }

  private String resolveCurrentTeacherName(CurrentPrincipal principal) {
    UserBasicInfo user = identityInternalApi.getUserById(principal.userId());
    if (user == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    if (!principal.tenantId().equals(user.tenantId())) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "User không thuộc tenant hiện tại");
    }
    return displayName(user);
  }

  private String resolveTeacherName(UUID teacherId) {
    if (teacherId == null) {
      return null;
    }
    return displayName(identityInternalApi.getUserById(teacherId));
  }

  private String displayName(UserBasicInfo user) {
    if (user == null) {
      return null;
    }
    if (user.fullName() != null && !user.fullName().isBlank()) {
      return user.fullName();
    }
    return user.username();
  }

  private void validateRulePayload(String rulePayload) {
    try {
      objectMapper.readTree(rulePayload);
    } catch (JsonProcessingException e) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "rulePayload phải là JSON hợp lệ");
    }
  }

  private CurrentPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)
        || !authentication.isAuthenticated()) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    Jwt jwt = jwtAuth.getToken();
    return new CurrentPrincipal(parseUuidClaim(jwt, "tenant_id"), parseUuidClaim(jwt, "user_id"));
  }

  private UUID parseUuidClaim(Jwt jwt, String claimName) {
    String raw = jwt.getClaimAsString(claimName);
    if (raw == null || raw.isBlank()) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private record CurrentPrincipal(UUID tenantId, UUID userId) {}
}
