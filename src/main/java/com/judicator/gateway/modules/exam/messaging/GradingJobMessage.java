package com.judicator.gateway.modules.exam.messaging;

import java.util.UUID;

/**
 * JSON message published to RabbitMQ queue {@code grading_jobs} for each student submission found
 * inside the batch ZIP file.
 *
 * <p>The grading runner service consumes this message and uploads its result via the internal
 * webhook {@code PUT /internal/submissions/{submissionId}/result}.
 *
 * @param submissionId the UUID of the persisted {@code ExamSubmission} record
 * @param objectKey the MinIO object key pointing to the student's individual source ZIP (e.g.,
 *     {@code tenantId/exams/examId/student-folder/source.zip})
 */
public record GradingJobMessage(UUID submissionId, String objectKey) {}
