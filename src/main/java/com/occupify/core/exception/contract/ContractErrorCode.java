package com.occupify.core.exception.contract;

import com.occupify.core.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ContractErrorCode implements BaseErrorCode {
  CONTRACT_NOT_FOUND("CONTRACT_001", "Contract not found", HttpStatus.NOT_FOUND),
  CONTRACT_ALREADY_ACTIVE(
      "CONTRACT_002",
      "Hợp đồng đã kích hoạt hoặc không ở trạng thái chờ duyệt",
      HttpStatus.BAD_REQUEST),
  JOB_NOT_OWNED(
      "CONTRACT_003", "Job does not belong to employer's project", HttpStatus.BAD_REQUEST),
  USER_NOT_FOUND("CONTRACT_004", "User not found", HttpStatus.BAD_REQUEST),
  INVALID_CANCELLATION_REASON(
      "CONTRACT_005", "Lý do hủy hợp đồng không hợp lệ hoặc quá ngắn", HttpStatus.BAD_REQUEST),
  CONTRACT_ALREADY_COMPLETED(
      "CONTRACT_006",
      "Hợp đồng đã hoàn thành, không thể thay đổi hoặc hủy",
      HttpStatus.BAD_REQUEST),
  CONTRACT_ALREADY_CANCELLED("CONTRACT_007", "Hợp đồng đã bị hủy", HttpStatus.BAD_REQUEST),
  FORBIDDEN_CONTRACT_ACCESS(
      "CONTRACT_008",
      "Bạn không có quyền truy cập hoặc thực hiện thao tác trên hợp đồng này",
      HttpStatus.FORBIDDEN);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
