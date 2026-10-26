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
  FILE_TOO_LARGE(
      "STORAGE_002", "Uploaded file exceeds maximum allowed limit (10MB)", HttpStatus.BAD_REQUEST),
  INVALID_FILE_TYPE(
      "STORAGE_003",
      "Invalid file type. Only PDF, DOC, DOCX, PNG, and JPEG documents are allowed",
      HttpStatus.BAD_REQUEST),
  UPLOAD_FAILED(
      "STORAGE_004", "Failed to upload file to storage", HttpStatus.INTERNAL_SERVER_ERROR),
  PRESIGN_FAILED(
      "STORAGE_005", "Failed to generate presigned URL", HttpStatus.INTERNAL_SERVER_ERROR),
  DELETE_FAILED(
      "STORAGE_006", "Failed to delete file from storage", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_DOCUMENT_TYPE(
      "STORAGE_007",
      "Only PDF, JPEG, PNG, and WEBP documents are allowed for attachments",
      HttpStatus.BAD_REQUEST),
  DOCUMENT_TOO_LARGE(
      "STORAGE_008",
      "Uploaded document exceeds maximum allowed limit (10MB)",
      HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
