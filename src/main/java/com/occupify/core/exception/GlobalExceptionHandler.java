package com.occupify.core.exception;

import com.occupify.core.dto.base.ErrorResponse;
import com.occupify.core.exception.system.SystemErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
    log.warn("Base exception: [{}] - {}", ex.getCode(), ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(ex.getHttpStatus().value(), ex.getMessage(), ex.getCode());
    return ResponseEntity.status(ex.getHttpStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    String firstError =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse(SystemErrorCode.VALIDATION_ERROR.message());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), firstError, SystemErrorCode.VALIDATION_ERROR.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
    log.warn("Binding validation failed: {}", ex.getMessage());
    String firstError =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse(SystemErrorCode.VALIDATION_ERROR.message());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), firstError, SystemErrorCode.VALIDATION_ERROR.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    log.warn("Method not supported: {} for {}", ex.getMethod(), ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(
            SystemErrorCode.METHOD_NOT_ALLOWED.httpStatus().value(),
            SystemErrorCode.METHOD_NOT_ALLOWED.message(),
            SystemErrorCode.METHOD_NOT_ALLOWED.code());
    return ResponseEntity.status(SystemErrorCode.METHOD_NOT_ALLOWED.httpStatus()).body(response);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
    log.warn("Resource not found: {}", ex.getResourcePath());
    ErrorResponse response =
        new ErrorResponse(
            SystemErrorCode.RESOURCE_NOT_FOUND.httpStatus().value(),
            SystemErrorCode.RESOURCE_NOT_FOUND.message(),
            SystemErrorCode.RESOURCE_NOT_FOUND.code());
    return ResponseEntity.status(SystemErrorCode.RESOURCE_NOT_FOUND.httpStatus()).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(
            SystemErrorCode.INVALID_PARAMETER.httpStatus().value(),
            ex.getMessage(),
            SystemErrorCode.INVALID_PARAMETER.code());
    return ResponseEntity.status(SystemErrorCode.INVALID_PARAMETER.httpStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unhandled server exception: ", ex);
    ErrorResponse response =
        new ErrorResponse(
            SystemErrorCode.INTERNAL_ERROR.httpStatus().value(),
            SystemErrorCode.INTERNAL_ERROR.message(),
            SystemErrorCode.INTERNAL_ERROR.code());
    return ResponseEntity.status(SystemErrorCode.INTERNAL_ERROR.httpStatus()).body(response);
  }
}
