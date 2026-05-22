package com.judicator.gateway.modules.exam.service;

import com.judicator.gateway.modules.exam.dto.request.BatchGradeRequest;
import com.judicator.gateway.modules.exam.dto.request.SubmissionResultRequest;
import com.judicator.gateway.modules.exam.dto.response.BatchGradeResponse;
import java.util.UUID;

/** Service contract for the Batch Grading workflow. */
public interface BatchGradingService {

  /**
   * Triggers a batch grading run for the given exam.
   *
   * <p>Downloads the ZIP from MinIO, extracts student folders, creates {@code ExamSubmission}
   * records in PostgreSQL, and publishes a {@code GradingJobMessage} to RabbitMQ for each
   * submission.
   *
   * @param examId the exam being graded
   * @param request contains the MinIO {@code objectKey} of the uploaded batch ZIP
   * @return a summary of how many submissions were queued
   */
  BatchGradeResponse triggerBatchGrading(UUID examId, BatchGradeRequest request);

  /**
   * Webhook handler called by the grading runner when it finishes scoring a single submission.
   *
   * <p>Updates {@code autoScore}, {@code finalScore}, and {@code status} on the matching {@code
   * ExamSubmission}. This endpoint bypasses tenant-scoped filters because it is an internal
   * service-to-service call, not a user-facing request.
   *
   * @param submissionId the UUID of the {@code ExamSubmission} to update
   * @param request score payload from the runner
   */
  void recordGradingResult(UUID submissionId, SubmissionResultRequest request);
}
