package com.occupify.core.controller;

import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.base.SuccessResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseFactory {

  public <T> PageResponse<T> createPageResponse(
      String message, List<T> data, int page, int limit, long total, int totalPages) {
    return new PageResponse<>(
        HttpStatus.OK.value(),
        message,
        data,
        new PageResponse.PagingInfo(page, limit, total, totalPages));
  }

  public <T> ResponseEntity<SingleResponse<T>> successSingle(T data, String message) {
    return ResponseEntity.ok(new SingleResponse<>(HttpStatus.OK.value(), message, data));
  }

  public <T> ResponseEntity<SingleResponse<T>> accepted(T data, String message) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new SingleResponse<>(HttpStatus.ACCEPTED.value(), message, data));
  }

  public <T> ResponseEntity<CreatedResponse<T>> created(T data, String message) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreatedResponse<>(HttpStatus.CREATED.value(), message, data));
  }

  public ResponseEntity<SuccessResponse> success(String message) {
    return ResponseEntity.ok(new SuccessResponse(HttpStatus.OK.value(), message));
  }
}
