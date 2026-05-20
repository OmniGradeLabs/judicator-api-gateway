package com.judicator.gateway.modules.exam.controller;

import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.common.response.ApiResponse;
import com.judicator.gateway.modules.exam.dto.request.BatchGradeRequest;
import com.judicator.gateway.modules.exam.dto.response.BatchGradeResponse;
import com.judicator.gateway.modules.exam.service.BatchGradingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Teacher-facing endpoint to trigger a Batch Grading run for an exam.
 *
 * <p>The teacher first uploads a batch ZIP to MinIO via the existing presigned-URL endpoint, then
 * calls this endpoint with the returned {@code objectKey}. The server responds immediately with
 * HTTP 200 — grading happens asynchronously via RabbitMQ.
 */
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(
    name = "Exam — Batch Grading",
    description = "Trigger batch grading for an exam submission set")
public class BatchGradingController {

  BatchGradingService batchGradingService;

  @Operation(
      summary = "Trigger Batch Grading",
      description =
          "Tải ZIP từ MinIO, trích xuất thư mục của từng sinh viên, tạo ExamSubmission records"
              + " trong DB và đẩy job vào RabbitMQ. Trả về ngay lập tức — grading chạy bất đồng bộ.")
  @PostMapping("/{examId}/batch-grade")
  public ResponseEntity<ApiResponse<BatchGradeResponse>> triggerBatchGrading(
      @PathVariable UUID examId, @Valid @RequestBody BatchGradeRequest request) {

    BatchGradeResponse result = batchGradingService.triggerBatchGrading(examId, request);

    return ResponseEntity.ok(
        ApiResponse.<BatchGradeResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Batch grading đã được khởi động thành công")
            .result(result)
            .timestamp(Instant.now())
            .build());
  }
}
