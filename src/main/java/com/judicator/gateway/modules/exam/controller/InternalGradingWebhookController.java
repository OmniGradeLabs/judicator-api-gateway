package com.judicator.gateway.modules.exam.controller;

import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.common.response.ApiResponse;
import com.judicator.gateway.modules.exam.dto.request.SubmissionResultRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal webhook controller called by the grading runner microservice.
 *
 * <p><strong>Security note:</strong> This controller is mounted under {@code /internal/} — this
 * prefix MUST be blocked at the API gateway / load balancer level so that it is not reachable from
 * the public internet. The endpoint is intentionally excluded from the standard tenant security
 * filter chain (it is an internal machine-to-machine call, not a user-facing request).
 *
 * <p>Add {@code /internal/**} to the permit-list in {@link
 * com.judicator.gateway.common.security.config.SecurityConfig} only for service-mesh / internal
 * network traffic.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(
    name = "Internal — Grading Webhook",
    description =
        "Internal webhook called by the grading runner. NOT accessible from the internet.")
public class InternalGradingWebhookController {

  BatchGradingService batchGradingService;

  @Operation(
      summary = "Report Grading Result (internal webhook)",
      description =
          "Được gọi bởi grading runner sau khi chấm xong. Cập nhật autoScore, finalScore và"
              + " chuyển status sang GRADED. Endpoint này không yêu cầu JWT của người dùng.")
  @PutMapping("/submissions/{submissionId}/result")
  public ResponseEntity<ApiResponse<Void>> recordGradingResult(
      @PathVariable UUID submissionId, @Valid @RequestBody SubmissionResultRequest request) {

    batchGradingService.recordGradingResult(submissionId, request);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Kết quả chấm điểm đã được cập nhật thành công")
            .timestamp(Instant.now())
            .build());
  }
}
