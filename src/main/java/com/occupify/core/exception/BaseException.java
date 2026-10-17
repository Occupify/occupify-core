package com.occupify.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

  private final String code;
  private final HttpStatus httpStatus;

  protected BaseException(BaseErrorCode errorCode) {
    super(errorCode.message());
    this.code = errorCode.code();
    this.httpStatus = errorCode.httpStatus();
  }

  protected BaseException(BaseErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.code = errorCode.code();
    this.httpStatus = errorCode.httpStatus();
  }
}
