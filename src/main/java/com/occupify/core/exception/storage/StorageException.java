package com.occupify.core.exception.storage;

import com.occupify.core.exception.BaseException;
import lombok.Getter;

@Getter
public class StorageException extends BaseException {

  private final StorageErrorCode errorCode;

  public StorageException(StorageErrorCode errorCode) {
    super(errorCode);
    this.errorCode = errorCode;
  }

  public StorageException(StorageErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
    this.errorCode = errorCode;
  }

  public StorageException(StorageErrorCode errorCode, String customMessage, Throwable cause) {
    super(errorCode, customMessage);
    this.errorCode = errorCode;
    if (cause != null) {
      initCause(cause);
    }
  }

  public StorageException(StorageErrorCode errorCode, Throwable cause) {
    super(errorCode, errorCode.message());
    this.errorCode = errorCode;
    if (cause != null) {
      initCause(cause);
    }
  }
}
