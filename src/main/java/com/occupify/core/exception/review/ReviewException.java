package com.occupify.core.exception.review;

import com.occupify.core.exception.BaseException;

public class ReviewException extends BaseException {

  public ReviewException(ReviewErrorCode errorCode) {
    super(errorCode);
  }

  public ReviewException(ReviewErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }
}
