package com.judicator.gateway.modules.exam.dto.response;

import com.judicator.gateway.modules.exam.enumType.ExamStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ExamResponse(
    UUID id,
    UUID tenantId,
    String title,
    String markdown,
    Integer timeLimitMinutes,
    String paceLimits,
    ExamStatus status,
    UUID createdByTeacherId,
    String createdByTeacherName,
    String rulePayload,
    String playwrightZipUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
