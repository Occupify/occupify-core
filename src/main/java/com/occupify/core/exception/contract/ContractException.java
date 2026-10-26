package com.occupify.core.exception.contract;

import com.occupify.core.exception.BaseException;

public class ContractException extends BaseException {

  public ContractException(ContractErrorCode errorCode) {
    super(errorCode);
  }

  public ContractException(ContractErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }
}
