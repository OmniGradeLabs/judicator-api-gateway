package com.judicator.gateway.modules.exam.service.impl;

import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.infrastructure.messaging.config.RabbitMQConfig;
import com.judicator.gateway.infrastructure.storage.service.MinioStorageService;
import com.judicator.gateway.modules.exam.dto.request.BatchGradeRequest;
import com.judicator.gateway.modules.exam.dto.request.SubmissionResultRequest;
import com.judicator.gateway.modules.exam.dto.response.BatchGradeResponse;
import com.judicator.gateway.modules.exam.entity.Exam;
import com.judicator.gateway.modules.exam.entity.ExamSubmission;
import com.judicator.gateway.modules.exam.enumType.SubmissionStatus;
import com.judicator.gateway.modules.exam.messaging.GradingJobMessage;
import com.judicator.gateway.modules.exam.repository.ExamRepository;
import com.judicator.gateway.modules.exam.repository.ExamSubmissionRepository;
import com.judicator.gateway.modules.exam.service.BatchGradingService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Batch Grading workflow.
 *
 * <p><strong>Flow:</strong>
 *
 * <ol>
 *   <li>Download the batch ZIP from MinIO via {@link MinioStorageService#downloadObject}.
 *   <li>Walk the ZIP entries in memory with {@link ZipInputStream} — no temp files on disk.
 *   <li>For each top-level student folder found, create an {@link ExamSubmission} with status
 *       {@code PENDING} and persist it in PostgreSQL inside a single transaction.
 *   <li>After the transaction commits, publish one {@link GradingJobMessage} per submission to the
 *       {@code grading_jobs} RabbitMQ queue. Publishing is intentionally done OUTSIDE the
 *       transaction to avoid sending messages for records that may roll back.
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BatchGradingServiceImpl implements BatchGradingService {

  MinioStorageService minioStorageService;
  ExamRepository examRepository;
  ExamSubmissionRepository examSubmissionRepository;
  RabbitTemplate rabbitTemplate;

  // ── triggerBatchGrading ────────────────────────────────────────────────────

  @Override
  public BatchGradeResponse triggerBatchGrading(UUID examId, BatchGradeRequest request) {
    log.info(
        "[BatchGrading] Bắt đầu batch grading cho examId: {}, objectKey: {}",
        examId,
        request.objectKey());

    // 1. Verify exam exists
    Exam exam =
        examRepository
            .findById(examId)
            .orElseThrow(
                () -> {
                  log.warn("[BatchGrading] Không tìm thấy exam ID: {}", examId);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài thi");
                });

    // 2. Discover student folder names from ZIP (in memory — no disk writes)
    Set<String> studentFolders = extractStudentFolders(request.objectKey());

    if (studentFolders.isEmpty()) {
      log.warn(
          "[BatchGrading] ZIP không chứa student folder nào — objectKey: {}", request.objectKey());
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "ZIP không chứa thư mục sinh viên hợp lệ nào");
    }

    // 3. Persist ExamSubmission records inside a transaction
    List<ExamSubmission> saved = persistSubmissions(exam, studentFolders, request.objectKey());

    // 4. Publish RabbitMQ messages AFTER transaction commits — avoids phantom messages on rollback
    publishGradingJobs(saved, request.objectKey());

    log.info(
        "[BatchGrading] Hoàn thành trigger — examId: {}, submissionsQueued: {}",
        examId,
        saved.size());

    return new BatchGradeResponse(
        examId,
        saved.size(),
        "Batch grading đã được khởi động. "
            + saved.size()
            + " submissions đã được đưa vào hàng đợi.");
  }

  // ── recordGradingResult ────────────────────────────────────────────────────

  @Override
  @Transactional
  public void recordGradingResult(UUID submissionId, SubmissionResultRequest request) {
    log.info(
        "[GradingWebhook] Nhận kết quả cho submissionId: {}, finalScore: {}",
        submissionId,
        request.finalScore());

    // Note: @SQLRestriction("is_deleted = false") does NOT apply to ExamSubmission,
    // so this query is unrestricted — appropriate for an internal webhook.
    ExamSubmission submission =
        examSubmissionRepository
            .findById(submissionId)
            .orElseThrow(
                () -> {
                  log.warn("[GradingWebhook] Không tìm thấy submission ID: {}", submissionId);
                  return new ApiException(
                      ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy submission");
                });

    submission.setAutoScore(request.autoScore());
    submission.setFinalScore(request.finalScore());
    submission.setStatus(SubmissionStatus.GRADED);
    examSubmissionRepository.save(submission);

    log.info(
        "[GradingWebhook] Cập nhật thành công submissionId: {} → GRADED, finalScore: {}",
        submissionId,
        request.finalScore());
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Streams the ZIP from MinIO and collects all distinct top-level directory names. Each top-level
   * directory is treated as one student submission folder.
   *
   * <p>The ZIP content is never written to disk — we only read entry names.
   */
  private Set<String> extractStudentFolders(String objectKey) {
    Set<String> folders = new HashSet<>();

    try (InputStream raw = minioStorageService.downloadObject(objectKey);
        ZipInputStream zis = new ZipInputStream(raw)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();

        // Guard against zip-slip path traversal
        if (name.contains("..") || name.startsWith("/")) {
          log.warn("[BatchGrading] ZIP entry bị loại do path traversal: {}", name);
          zis.closeEntry();
          continue;
        }

        // A top-level directory entry looks like "studentId/" or "studentId/anything"
        int slashIdx = name.indexOf('/');
        if (slashIdx > 0) {
          folders.add(name.substring(0, slashIdx));
        }

        zis.closeEntry();
      }
    } catch (ApiException e) {
      throw e;
    } catch (IOException e) {
      log.error("[BatchGrading] Lỗi đọc ZIP từ MinIO objectKey: {}", objectKey, e);
      throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Không thể đọc file ZIP từ storage");
    }

    log.debug("[BatchGrading] Tìm thấy {} student folder(s) trong ZIP", folders.size());
    return folders;
  }

  /**
   * Persists one {@link ExamSubmission} per student folder inside a single database transaction.
   * The student folder name is used as the {@code sourceFileUrl} path component so the runner can
   * locate the correct sub-archive inside the batch ZIP.
   */
  @Transactional
  protected List<ExamSubmission> persistSubmissions(
      Exam exam, Set<String> studentFolders, String batchObjectKey) {

    List<ExamSubmission> submissions = new ArrayList<>(studentFolders.size());

    for (String folder : studentFolders) {
      // Derive a per-student object key: same bucket path but scoped to the student folder
      String studentObjectKey = batchObjectKey.replace(".zip", "/" + folder + "/source.zip");

      // Attempt to parse folder name as UUID for studentId; fall back to a deterministic UUID
      UUID studentId = parseStudentId(folder);

      ExamSubmission submission =
          ExamSubmission.builder()
              .exam(exam)
              .studentId(studentId)
              .sourceFileUrl(studentObjectKey)
              .status(SubmissionStatus.PENDING)
              .submittedAt(Instant.now())
              .build();

      submissions.add(examSubmissionRepository.save(submission));
      log.debug(
          "[BatchGrading] Đã lưu submission cho folder '{}', ID: {}", folder, submission.getId());
    }

    return submissions;
  }

  /**
   * Publishes one {@link GradingJobMessage} per saved submission to the RabbitMQ exchange. Called
   * AFTER the transaction in {@link #persistSubmissions} has committed so we never publish messages
   * for records that may have rolled back.
   */
  private void publishGradingJobs(List<ExamSubmission> submissions, String batchObjectKey) {
    for (ExamSubmission s : submissions) {
      GradingJobMessage message = new GradingJobMessage(s.getId(), s.getSourceFileUrl());
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXAM_EXCHANGE, RabbitMQConfig.GRADING_ROUTING_KEY, message);
      log.debug("[BatchGrading] Published grading job message — submissionId: {}", s.getId());
    }
  }

  /** Parses the folder name as a UUID if possible; otherwise generates a name-based UUID v3. */
  private UUID parseStudentId(String folderName) {
    try {
      return UUID.fromString(folderName);
    } catch (IllegalArgumentException e) {
      // Deterministic: same folder name always yields the same UUID
      return UUID.nameUUIDFromBytes(folderName.getBytes());
    }
  }
}
