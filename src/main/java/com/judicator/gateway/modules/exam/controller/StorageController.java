package com.judicator.gateway.modules.exam.controller;

import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.common.response.ApiResponse;
import com.judicator.gateway.infrastructure.storage.dto.PresignedUploadUrl;
import com.judicator.gateway.infrastructure.storage.service.MinioStorageService;
import com.judicator.gateway.modules.exam.dto.storage.response.PresignedDownloadUrlResponse;
import com.judicator.gateway.modules.exam.dto.storage.response.PresignedUploadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "Storage", description = "Presigned URL cho upload/download file .zip")
@SecurityRequirement(name = "bearerAuth")
public class StorageController {

  MinioStorageService minioStorageService;

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/upload-url")
  @Operation(summary = "Tạo presigned URL để upload file .zip lên MinIO")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tạo upload URL thành công",
            content =
                @Content(schema = @Schema(implementation = PresignedUploadUrlResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "File không đúng định dạng (.zip) hoặc sai kiểu nội dung",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Chưa xác thực hoặc tenant_id không hợp lệ",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<PresignedUploadUrlResponse>> generateUploadUrl(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID examId,
      @RequestParam String fileName,
      @RequestParam(defaultValue = "application/zip") String contentType) {

    UUID tenantId = extractTenantId(jwt);
    PresignedUploadUrl presignedUploadUrl =
        minioStorageService.generateUploadUrlResponse(tenantId, examId, fileName, contentType);
    PresignedUploadUrlResponse result =
        new PresignedUploadUrlResponse(presignedUploadUrl.url(), presignedUploadUrl.objectKey());

    return ResponseEntity.ok(
        ApiResponse.<PresignedUploadUrlResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Tạo upload URL thành công")
            .result(result)
            .timestamp(Instant.now())
            .path("/storage/upload-url")
            .build());
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/download-url")
  @Operation(summary = "Tạo presigned URL để download file .zip từ MinIO")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tạo download URL thành công",
            content =
                @Content(schema = @Schema(implementation = PresignedDownloadUrlResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Object Key không hợp lệ (path traversal, v.v...)",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Không có quyền truy cập vào Object Key của Tenant khác",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<PresignedDownloadUrlResponse>> generateDownloadUrl(
      @AuthenticationPrincipal Jwt jwt, @RequestParam String objectKey) {

    UUID tenantId = extractTenantId(jwt);
    String url = minioStorageService.generateDownloadUrl(tenantId, objectKey);

    return ResponseEntity.ok(
        ApiResponse.<PresignedDownloadUrlResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Tạo download URL thành công")
            .result(new PresignedDownloadUrlResponse(url))
            .timestamp(Instant.now())
            .path("/storage/download-url")
            .build());
  }

  private UUID extractTenantId(Jwt jwt) {
    if (jwt == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    String tenantId = jwt.getClaimAsString("tenant_id");
    if (tenantId == null || tenantId.isBlank()) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    try {
      return UUID.fromString(tenantId);
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
