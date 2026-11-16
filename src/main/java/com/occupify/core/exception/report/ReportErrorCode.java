package com.occupify.core.exception.report;

import com.occupify.core.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {
  REPORT_NOT_FOUND("REPORT_001", "Report not found", HttpStatus.NOT_FOUND),
  CANNOT_REPORT_SELF("REPORT_002", "Cannot report yourself", HttpStatus.BAD_REQUEST),
  TARGET_USER_NOT_FOUND("REPORT_003", "Target user not found", HttpStatus.NOT_FOUND),
  TARGET_PROJECT_NOT_FOUND("REPORT_004", "Target project not found", HttpStatus.NOT_FOUND),
  TARGET_USER_REQUIRED(
      "REPORT_005", "Target user ID is required when target type is USER", HttpStatus.BAD_REQUEST),
  TARGET_PROJECT_REQUIRED(
      "REPORT_006",
      "Target project ID is required when target type is PROJECT",
      HttpStatus.BAD_REQUEST),
  REPORTER_NOT_FOUND("REPORT_007", "Reporter user not found", HttpStatus.NOT_FOUND),
  INVALID_REPORT_STATUS("REPORT_008", "Invalid report status", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
