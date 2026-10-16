package com.occupify.core.exception.system;

import com.occupify.core.exception.BaseException;

public class SystemException extends BaseException {

  public SystemException(SystemErrorCode errorCode) {
    super(errorCode);
  }

  public SystemException(SystemErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }
}
