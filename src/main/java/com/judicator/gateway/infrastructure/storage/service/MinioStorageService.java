package com.judicator.gateway.infrastructure.storage.service;

import com.judicator.gateway.infrastructure.storage.dto.PresignedUploadUrl;
import java.io.InputStream;
import java.util.UUID;

public interface MinioStorageService {

  String generateUploadUrl(UUID tenantId, UUID examId, String fileName, String contentType);

  PresignedUploadUrl generateUploadUrlResponse(
      UUID tenantId, UUID examId, String fileName, String contentType);

  String generateDownloadUrl(UUID tenantId, String objectKey);

  /**
   * Downloads an object from MinIO and returns its raw {@link InputStream}.
   *
   * <p>The caller is responsible for closing the stream (use try-with-resources). This method
   * bypasses presigned-URL generation and is intended for internal server-side consumption only
   * (e.g., batch grading ZIP extraction).
   *
   * @param objectKey the full MinIO object key (e.g., {@code tenantId/exams/examId/file.zip})
   * @return an open {@link InputStream} pointing to the object content
   */
  InputStream downloadObject(String objectKey);
}
