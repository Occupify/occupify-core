package com.occupify.core.exception.review;

import com.occupify.core.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {
  REVIEW_NOT_FOUND("REVIEW_001", "Review not found", HttpStatus.NOT_FOUND),
  CONTRACT_NOT_COMPLETED(
      "REVIEW_002", "Cannot review contract that is not completed", HttpStatus.BAD_REQUEST),
  DUPLICATE_REVIEW("REVIEW_003", "You have already reviewed this contract", HttpStatus.BAD_REQUEST),
  FORBIDDEN_REVIEW(
      "REVIEW_004", "You are not a participant of this contract", HttpStatus.FORBIDDEN),
  INVALID_REVIEW_TARGET(
      "REVIEW_005", "Cannot review yourself or invalid target", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
