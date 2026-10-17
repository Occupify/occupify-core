package com.occupify.core.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"statusCode", "message", "data", "paging"})
public record PageResponse<T>(Integer statusCode, String message, List<T> data, PagingInfo paging) {
  public record PagingInfo(int page, int limit, long total, int totalPages) {}
}
