package com.judicator.gateway.modules.exam.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for triggering a batch grading job.
 *
 * @param objectKey the full MinIO object key of the uploaded ZIP file (e.g., {@code
 *     tenantId/exams/examId/submissions.zip})
 */
public record BatchGradeRequest(
    @NotBlank(message = "objectKey không được để trống") String objectKey) {}
