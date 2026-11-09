package com.occupify.core.exception.report;

import com.occupify.core.exception.BaseException;

public class ReportException extends BaseException {

  public ReportException(ReportErrorCode errorCode) {
    super(errorCode);
  }

  public ReportException(ReportErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }
}
