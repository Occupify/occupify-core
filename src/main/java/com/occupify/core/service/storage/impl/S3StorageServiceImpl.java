package com.occupify.core.service.storage.impl;

import com.occupify.core.config.S3Properties;
import com.occupify.core.constant.StorageConstants;
import com.occupify.core.exception.storage.StorageErrorCode;
import com.occupify.core.exception.storage.StorageException;
import com.occupify.core.service.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

  private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10 MB
  private static final Set<String> ALLOWED_DOCUMENT_TYPES =
      Set.of("application/pdf", "image/jpeg", "image/jpg", "image/png", "image/webp");

  private final S3Client s3Client;
  private final S3Properties properties;

  @Override
  public String uploadDocument(String directory, MultipartFile file) {
    validateDocumentFile(file);

    String originalFilename = file.getOriginalFilename();
    String extension = extractExtension(originalFilename, file.getContentType());
    String cleanDirectory =
        directory != null ? directory.replaceAll("^/+|/+$", "") : "documents";
    String key = String.format("%s/%s%s", cleanDirectory, UUID.randomUUID(), extension);

    log.info(
        "Uploading document to S3: bucket={}, key={}, size={} bytes, contentType={}",
        properties.getBucketName(),
        key,
        file.getSize(),
        file.getContentType());

    try (InputStream inputStream = file.getInputStream()) {
      PutObjectRequest putRequest =
          PutObjectRequest.builder()
              .bucket(properties.getBucketName())
              .key(key)
              .contentType(file.getContentType())
              .contentLength(file.getSize())
              .build();

      s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
      String publicUrl = constructPublicUrl(key);
      log.info("Successfully uploaded document. Public URL: {}", publicUrl);
      return publicUrl;
    } catch (IOException e) {
      log.error("Failed to read input stream for document upload: {}", originalFilename, e);
      throw new StorageException(
          StorageErrorCode.UPLOAD_FAILED, "Failed to read uploaded file", e);
    } catch (Exception e) {
      log.error(
          "Failed to upload document to S3: bucket={}, key={}",
          properties.getBucketName(),
          key,
          e);
      throw new StorageException(
          StorageErrorCode.UPLOAD_FAILED, "S3 upload failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteFile(String fileUrlOrKey) {
    if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) {
      return;
    }

    String key = extractObjectKey(fileUrlOrKey);
    if (key == null || key.isBlank()) {
      log.info("Skipping delete for non-S3/external URL: {}", fileUrlOrKey);
      return;
    }

    log.info("Deleting object from S3: bucket={}, key={}", properties.getBucketName(), key);
    try {
      DeleteObjectRequest deleteRequest =
          DeleteObjectRequest.builder().bucket(properties.getBucketName()).key(key).build();

      s3Client.deleteObject(deleteRequest);
      log.info("Successfully deleted object from S3: key={}", key);
    } catch (Exception e) {
      log.error(
          "Failed to delete object from S3: bucket={}, key={}", properties.getBucketName(), key, e);
      throw new StorageException(
          StorageErrorCode.DELETE_FAILED, "S3 delete failed: " + e.getMessage(), e);
    }
  }

  private void validateDocumentFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new StorageException(StorageErrorCode.FILE_EMPTY);
    }

    if (file.getSize() > MAX_DOCUMENT_SIZE) {
      throw new StorageException(StorageErrorCode.DOCUMENT_TOO_LARGE);
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
      throw new StorageException(StorageErrorCode.INVALID_DOCUMENT_TYPE);
    }
  }

  private String extractExtension(String filename, String contentType) {
    if (filename != null && filename.contains(".")) {
      String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
      if (ext.matches("^\\.[a-z0-9]+$")) {
        return ext;
      }
    }
    if ("application/pdf".equalsIgnoreCase(contentType)) return ".pdf";
    if ("image/png".equalsIgnoreCase(contentType)) return ".png";
    if ("image/webp".equalsIgnoreCase(contentType)) return ".webp";
    return ".jpg";
  }

  private String constructPublicUrl(String key) {
    if (properties.getPublicUrlPrefix() != null && !properties.getPublicUrlPrefix().isBlank()) {
      return String.format(
          "%s/%s", stripTrailingSlash(properties.getPublicUrlPrefix().trim()), key);
    }

    if (properties.getBucketNamespace() != null && !properties.getBucketNamespace().isBlank()) {
      String region =
          properties.getRegion() != null
              ? properties.getRegion().trim()
              : StorageConstants.DEFAULT_OCI_REGION;
      return String.format(
          StorageConstants.OCI_PUBLIC_URL_FORMAT,
          region,
          properties.getBucketNamespace().trim(),
          properties.getBucketName().trim(),
          key);
    }

    if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
      return String.format(
          "%s/%s/%s",
          stripTrailingSlash(properties.getEndpoint().trim()),
          properties.getBucketName().trim(),
          key);
    }

    String region =
        properties.getRegion() != null
            ? properties.getRegion().trim()
            : StorageConstants.DEFAULT_AWS_REGION;
    return String.format(
        StorageConstants.AWS_PUBLIC_URL_FORMAT, properties.getBucketName().trim(), region, key);
  }

  @Nullable
  private String extractObjectKey(String fileUrlOrKey) {
    if (!fileUrlOrKey.startsWith("http://") && !fileUrlOrKey.startsWith("https://")) {
      return fileUrlOrKey.replaceAll("^/+", "");
    }

    if (fileUrlOrKey.contains("/o/")) {
      return fileUrlOrKey.substring(fileUrlOrKey.indexOf("/o/") + 3);
    }

    String bucketMarker = "/" + properties.getBucketName() + "/";
    if (fileUrlOrKey.contains(bucketMarker)) {
      return fileUrlOrKey.substring(fileUrlOrKey.indexOf(bucketMarker) + bucketMarker.length());
    }

    if (properties.getPublicUrlPrefix() != null && !properties.getPublicUrlPrefix().isBlank()) {
      String prefix = stripTrailingSlash(properties.getPublicUrlPrefix().trim());
      if (fileUrlOrKey.startsWith(prefix)) {
        return fileUrlOrKey.substring(prefix.length()).replaceAll("^/+", "");
      }
    }

    return null;
  }

  private String stripTrailingSlash(String str) {
    return str != null ? str.replaceAll("/+$", "") : "";
  }
}
