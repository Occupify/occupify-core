package com.occupify.core.exception.storage;

import com.occupify.core.exception.BaseException;

public class StorageException extends BaseException {

  public StorageException(StorageErrorCode errorCode) {
    super(errorCode);
  }

  public StorageException(StorageErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }

  public StorageException(StorageErrorCode errorCode, String customMessage, Throwable cause) {
    super(errorCode, customMessage);
    if (cause != null) {
      initCause(cause);
    }
  }
}
