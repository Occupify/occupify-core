package com.occupify.core.exception.bid;

import com.occupify.core.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum BidErrorCode implements BaseErrorCode {
  BID_NOT_FOUND("BID_001", "Bid not found", HttpStatus.NOT_FOUND),
  FORBIDDEN_OPERATION(
      "BID_002", "You do not have permission to access or modify this bid", HttpStatus.FORBIDDEN),
  CANNOT_BID_OWN_PROJECT("BID_003", "You cannot bid on your own project", HttpStatus.BAD_REQUEST),
  DUPLICATE_BID("BID_004", "You have already submitted a bid for this job", HttpStatus.CONFLICT),
  BID_NOT_PENDING(
      "BID_005", "Only pending bids can be updated or cancelled", HttpStatus.BAD_REQUEST),
  JOB_NOT_FOUND("BID_006", "Job not found", HttpStatus.NOT_FOUND),
  JOB_NOT_RECRUITING("BID_007", "Job is not currently recruiting", HttpStatus.BAD_REQUEST),
  PROJECT_NOT_OPEN("BID_008", "Project is closed or expired", HttpStatus.BAD_REQUEST),
  FREELANCER_NOT_FOUND("BID_009", "Freelancer user not found", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
