package com.occupify.core.service.storage.impl;

import com.occupify.core.config.S3Properties;
import com.occupify.core.exception.storage.StorageErrorCode;
import com.occupify.core.exception.storage.StorageException;
import com.occupify.core.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

  private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10 MB

  private static final Set<String> ALLOWED_DOCUMENT_TYPES =
      Set.of(
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "image/jpeg",
          "image/jpg",
          "image/png");

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Properties properties;

  @Override
  public String uploadPrivateDocument(String directory, MultipartFile file) {
    validateFile(file);

    String originalFilename = file.getOriginalFilename();
    String cleanFilename = sanitizeFilename(originalFilename);
    String cleanDirectory =
        StringUtils.hasText(directory) ? directory.replaceAll("^/+|/+$", "") : "documents";
    String objectKey = String.format("%s/%s_%s", cleanDirectory, UUID.randomUUID(), cleanFilename);

    log.info(
        "Uploading private document to S3: bucket={}, key={}, size={} bytes, contentType={}",
        properties.getBucketName(),
        objectKey,
        file.getSize(),
        file.getContentType());

    try (InputStream inputStream = file.getInputStream()) {
      PutObjectRequest putRequest =
          PutObjectRequest.builder()
              .bucket(properties.getBucketName())
              .key(objectKey)
              .contentType(file.getContentType())
              .contentLength(file.getSize())
              .build();

      s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
      log.info("Successfully uploaded private document with key: {}", objectKey);
      return objectKey;
    } catch (IOException e) {
      log.error("Failed to read input stream for file upload: {}", originalFilename, e);
      throw new StorageException(StorageErrorCode.UPLOAD_FAILED, "Failed to read uploaded file", e);
    } catch (Exception e) {
      log.error(
          "Failed to upload document to S3: bucket={}, key={}",
          properties.getBucketName(),
          objectKey,
          e);
      throw new StorageException(StorageErrorCode.UPLOAD_FAILED, e);
    }
  }

  @Override
  public String generatePresignedDownloadUrl(String objectKey, Duration duration) {
    if (!StringUtils.hasText(objectKey)) {
      throw new StorageException(StorageErrorCode.PRESIGN_FAILED, "Object key must not be blank");
    }

    try {
      Duration expiry =
          duration != null
              ? duration
              : Duration.ofMinutes(properties.getPresignedDurationMinutes());
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder()
              .bucket(properties.getBucketName())
              .key(objectKey.trim())
              .build();

      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(expiry)
              .getObjectRequest(getObjectRequest)
              .build();

      PresignedGetObjectRequest presignedGetObjectRequest =
          s3Presigner.presignGetObject(presignRequest);
      return presignedGetObjectRequest.url().toString();
    } catch (Exception e) {
      log.error("Failed to generate presigned URL for key: {}", objectKey, e);
      throw new StorageException(StorageErrorCode.PRESIGN_FAILED, e);
    }
  }

  @Override
  public void deleteFile(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      return;
    }

    try {
      DeleteObjectRequest deleteRequest =
          DeleteObjectRequest.builder()
              .bucket(properties.getBucketName())
              .key(objectKey.trim())
              .build();

      s3Client.deleteObject(deleteRequest);
      log.info("Deleted S3 object: bucket={}, key={}", properties.getBucketName(), objectKey);
    } catch (Exception e) {
      log.error(
          "Failed to delete S3 object: bucket={}, key={}",
          properties.getBucketName(),
          objectKey,
          e);
      throw new StorageException(StorageErrorCode.DELETE_FAILED, e);
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new StorageException(StorageErrorCode.FILE_EMPTY);
    }

    if (file.getSize() > MAX_DOCUMENT_SIZE) {
      throw new StorageException(StorageErrorCode.FILE_TOO_LARGE);
    }

    String contentType = file.getContentType();
    if (contentType == null || contentType.isBlank()) {
      throw new StorageException(StorageErrorCode.INVALID_FILE_TYPE);
    }

    String cleanContentType = contentType.split(";")[0].trim().toLowerCase();
    if (!ALLOWED_DOCUMENT_TYPES.contains(cleanContentType)) {
      throw new StorageException(StorageErrorCode.INVALID_FILE_TYPE);
    }
  }

  private String sanitizeFilename(String filename) {
    if (!StringUtils.hasText(filename)) {
      return "document";
    }
    String name = Paths.get(filename).getFileName().toString();
    return name.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
