package com.occupify.core.exception.project;

import com.occupify.core.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {
  PROJECT_NOT_FOUND("PROJ_001", "Project not found", HttpStatus.NOT_FOUND),
  FORBIDDEN_OPERATION(
      "PROJ_002", "You do not have permission to modify this project", HttpStatus.FORBIDDEN),
  PROJECT_ALREADY_CLOSED("PROJ_003", "Project is already closed", HttpStatus.BAD_REQUEST),
  EMPLOYER_NOT_FOUND("PROJ_004", "Employer user not found", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
