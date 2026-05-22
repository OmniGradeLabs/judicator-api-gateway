package com.judicator.gateway.infrastructure.storage.service.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.judicator.gateway.common.exception.ApiException;
import com.judicator.gateway.common.exception.ErrorCode;
import com.judicator.gateway.infrastructure.storage.dto.PresignedUploadUrl;
import com.judicator.gateway.infrastructure.storage.service.MinioStorageService;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MinioStorageServiceImpl implements MinioStorageService {

  static final long UPLOAD_URL_TTL_SECONDS = 15 * 60;
  static final long DOWNLOAD_URL_TTL_SECONDS = 60 * 60;
  static final String ZIP_CONTENT_TYPE = "application/zip";

  AmazonS3 amazonS3;

  @NonFinal
  @Value("${app.minio.bucket:judicator-bucket}")
  String bucket;

  @Override
  public String generateUploadUrl(UUID tenantId, UUID examId, String fileName, String contentType) {
    return generateUploadUrlResponse(tenantId, examId, fileName, contentType).url();
  }

  @Override
  public PresignedUploadUrl generateUploadUrlResponse(
      UUID tenantId, UUID examId, String fileName, String contentType) {
    validateUploadInput(tenantId, examId, fileName, contentType);

    String objectKey =
        String.format("%s/exams/%s/%s-%s", tenantId, examId, UUID.randomUUID(), fileName);

    GeneratePresignedUrlRequest request =
        new GeneratePresignedUrlRequest(bucket, objectKey)
            .withMethod(HttpMethod.PUT)
            .withContentType(contentType)
            .withExpiration(expirationAfterSeconds(UPLOAD_URL_TTL_SECONDS));

    URL url = amazonS3.generatePresignedUrl(request);
    return new PresignedUploadUrl(url.toString(), objectKey);
  }

  @Override
  public String generateDownloadUrl(UUID tenantId, String objectKey) {
    validateDownloadInput(tenantId, objectKey);

    GeneratePresignedUrlRequest request =
        new GeneratePresignedUrlRequest(bucket, objectKey)
            .withMethod(HttpMethod.GET)
            .withExpiration(expirationAfterSeconds(DOWNLOAD_URL_TTL_SECONDS));

    return amazonS3.generatePresignedUrl(request).toString();
  }

  /**
   * Downloads an object directly from MinIO and returns its raw {@link InputStream}. Intended for
   * internal server-side use only (e.g., batch grading ZIP extraction). The caller must close the
   * stream when done.
   */
  @Override
  public InputStream downloadObject(String objectKey) {
    if (isBlank(objectKey)) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Object key không được để trống");
    }
    try {
      S3Object s3Object = amazonS3.getObject(bucket, objectKey);
      return s3Object.getObjectContent();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy file trên storage");
    }
  }

  private void validateUploadInput(
      UUID tenantId, UUID examId, String fileName, String contentType) {
    if (tenantId == null || examId == null || isBlank(fileName) || isBlank(contentType)) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Thông tin file upload không hợp lệ");
    }

    if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Tên file không hợp lệ");
    }

    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Chỉ hỗ trợ file .zip");
    }

    if (!ZIP_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
      throw new ApiException(
          ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Content-Type phải là application/zip");
    }
  }

  private void validateDownloadInput(UUID tenantId, String objectKey) {
    if (tenantId == null || isBlank(objectKey)) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Object key không hợp lệ");
    }

    String tenantPrefix = tenantId + "/";
    if (!objectKey.startsWith(tenantPrefix)) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "Unauthorized access to object key");
    }

    String examPrefix = tenantId + "/exams/";
    if (!objectKey.startsWith(examPrefix)
        || objectKey.contains("\\")
        || objectKey.contains("..")
        || !objectKey.toLowerCase(Locale.ROOT).endsWith(".zip")) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Object key không hợp lệ");
    }

    String remainingKey = objectKey.substring(examPrefix.length());
    int separatorIndex = remainingKey.indexOf('/');
    if (separatorIndex <= 0 || separatorIndex == remainingKey.length() - 1) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Object key không hợp lệ");
    }

    try {
      UUID.fromString(remainingKey.substring(0, separatorIndex));
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Object key không hợp lệ");
    }
  }

  private Date expirationAfterSeconds(long seconds) {
    return Date.from(Instant.now().plusSeconds(seconds));
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
