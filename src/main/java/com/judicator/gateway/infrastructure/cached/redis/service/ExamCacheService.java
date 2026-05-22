package com.judicator.gateway.infrastructure.cached.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.judicator.gateway.infrastructure.cached.redis.helper.RedisJsonCacheSupport;
import com.judicator.gateway.infrastructure.cached.redis.keys.RedisKeys;
import com.judicator.gateway.modules.exam.dto.response.ExamResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExamCacheService extends RedisJsonCacheSupport {

  private static final Duration DETAIL_TTL = Duration.ofMinutes(30);
  private static final Duration LIST_TTL = Duration.ofMinutes(10);
  private static final TypeReference<List<ExamResponse>> EXAM_LIST_TYPE = new TypeReference<>() {};

  public ExamCacheService(StringRedisTemplate redis, ObjectMapper redisObjectMapper) {
    super(redis, redisObjectMapper);
  }

  public Optional<ExamResponse> getExam(UUID tenantId, UUID examId) {
    String cacheKey = RedisKeys.examDetail(tenantId, examId);
    return readRawValue(cacheKey).flatMap(json -> deserialize(json, ExamResponse.class));
  }

  public void saveExam(UUID tenantId, UUID examId, ExamResponse response) {
    String cacheKey = RedisKeys.examDetail(tenantId, examId);
    serialize(response).ifPresent(json -> writeValue(cacheKey, json, DETAIL_TTL));
  }

  public Optional<List<ExamResponse>> getExamList(UUID tenantId) {
    String cacheKey = RedisKeys.examList(tenantId);
    return readRawValue(cacheKey).flatMap(json -> deserialize(json, EXAM_LIST_TYPE));
  }

  public void saveExamList(UUID tenantId, List<ExamResponse> responses) {
    String cacheKey = RedisKeys.examList(tenantId);
    serialize(responses).ifPresent(json -> writeValue(cacheKey, json, LIST_TTL));
  }

  public void evictExam(UUID tenantId, UUID examId) {
    deleteKey(RedisKeys.examDetail(tenantId, examId));
  }

  public void evictExamList(UUID tenantId) {
    deleteKey(RedisKeys.examList(tenantId));
  }

  public void evictTenantExamData(UUID tenantId) {
    scanAndDelete(RedisKeys.examPattern(tenantId));
  }
}
