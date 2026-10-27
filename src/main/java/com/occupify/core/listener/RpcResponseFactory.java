package com.occupify.core.listener;

import com.occupify.core.dto.base.ErrorResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.base.SuccessResponse;
import com.occupify.core.exception.BaseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RpcResponseFactory {

  public <T> PageResponse<T> page(
      String message, List<T> data, int page, int limit, long total, int totalPages) {
    return new PageResponse<>(
        HttpStatus.OK.value(),
        message,
        data,
        new PageResponse.PagingInfo(page, limit, total, totalPages));
  }

  public <T> SingleResponse<T> single(T data, String message) {
    return new SingleResponse<>(HttpStatus.OK.value(), message, data);
  }

  public SuccessResponse success(String message) {
    return new SuccessResponse(HttpStatus.OK.value(), message);
  }

  public ErrorResponse error(int statusCode, String message, String errorCode) {
    return new ErrorResponse(statusCode, message, errorCode);
  }

  public ErrorResponse error(BaseException ex) {
    return new ErrorResponse(ex.getHttpStatus().value(), ex.getMessage(), ex.getCode());
  }
}
