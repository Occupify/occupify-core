package com.occupify.core.exception.storage;

import com.occupify.core.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum StorageErrorCode implements BaseErrorCode {
  FILE_EMPTY("STORAGE_001", "Uploaded file is empty or missing", HttpStatus.BAD_REQUEST),
  INVALID_FILE_TYPE(
      "STORAGE_002", "Only JPEG, PNG, WEBP, and GIF images are allowed", HttpStatus.BAD_REQUEST),
  FILE_TOO_LARGE(
      "STORAGE_003", "Uploaded file exceeds maximum allowed limit (5MB)", HttpStatus.BAD_REQUEST),
  UPLOAD_FAILED(
      "STORAGE_004", "Failed to upload file to object storage", HttpStatus.INTERNAL_SERVER_ERROR),
  DELETE_FAILED(
      "STORAGE_005", "Failed to delete file from object storage", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_DOCUMENT_TYPE(
      "STORAGE_006",
      "Only PDF, JPEG, PNG, and WEBP documents are allowed for attachments",
      HttpStatus.BAD_REQUEST),
  DOCUMENT_TOO_LARGE(
      "STORAGE_007",
      "Uploaded document exceeds maximum allowed limit (10MB)",
      HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
