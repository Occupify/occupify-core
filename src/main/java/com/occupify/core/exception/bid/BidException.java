package com.occupify.core.exception.bid;

import com.occupify.core.exception.BaseException;

public class BidException extends BaseException {

  public BidException(BidErrorCode errorCode) {
    super(errorCode);
  }

  public BidException(BidErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }
}
