package com.occupify.core.service;

import java.time.Duration;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  String uploadDocument(String directory, MultipartFile file);

  /**
   * @param directory target directory/prefix (e.g. "contracts/123")
   * @param file the uploaded file
   * @return the generated S3 object key (e.g. "contracts/123/uuid_filename.pdf")
   */
  String uploadPrivateDocument(String directory, MultipartFile file);

  /**
   * Generates a time-limited presigned GET URL for downloading/viewing a private document.
   *
   * @param objectKey the S3 object key
   * @param duration time to live for the presigned URL
   * @return time-limited signed URL string
   */
  String generatePresignedDownloadUrl(String objectKey, Duration duration);

  /**
   * Deletes an object from storage by its object key.
   *
   * @param objectKey the S3 object key
   */
  void deleteFile(String objectKey);
}
