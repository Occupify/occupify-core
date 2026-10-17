package com.occupify.core.exception.project;

import com.occupify.core.exception.BaseException;

public class ProjectException extends BaseException {

  public ProjectException(ProjectErrorCode errorCode) {
    super(errorCode);
  }

  public ProjectException(ProjectErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }
}
