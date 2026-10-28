package com.occupify.core.exception;

import com.occupify.core.dto.base.ErrorResponse;
import com.occupify.core.exception.system.SystemErrorCode;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {
    log.warn("Media type not supported: {}", ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            ex.getMessage(),
            SystemErrorCode.INVALID_MULTIPART.code());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(
      HttpMessageNotReadableException ex) {
    log.warn("Message not readable: {}", ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Malformed request body or part: " + ex.getMostSpecificCause().getMessage(),
            SystemErrorCode.INVALID_PARAMETER.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    log.warn(
        "Type mismatch for parameter '{}': value='{}', expectedType='{}'",
        ex.getName(),
        ex.getValue(),
        ex.getRequiredType());
    String requiredTypeName =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid value";
    String message =
        String.format(
            "Invalid parameter '%s': '%s' is not a valid %s",
            ex.getName(), ex.getValue(), requiredTypeName);
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), message, SystemErrorCode.INVALID_PARAMETER.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
      MissingServletRequestPartException ex) {
    log.warn("Missing required multipart part: {}", ex.getRequestPartName());
    String message =
        String.format("Required multipart part '%s' is missing", ex.getRequestPartName());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), message, SystemErrorCode.INVALID_MULTIPART.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex) {
    log.warn("Missing request parameter: {}", ex.getParameterName());
    String message =
        String.format("Required query parameter '%s' is missing", ex.getParameterName());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), message, SystemErrorCode.INVALID_PARAMETER.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());
    String message =
        ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElse(SystemErrorCode.VALIDATION_ERROR.message());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), message, SystemErrorCode.VALIDATION_ERROR.code());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex) {
    log.warn("File upload size exceeded: {}", ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "Uploaded file exceeds maximum allowed size",
            SystemErrorCode.INVALID_MULTIPART.code());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMessage());
    ErrorResponse response =
        new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Data integrity violation: " + ex.getMostSpecificCause().getMessage(),
            SystemErrorCode.DATABASE_ERROR.code());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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
