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
  CONTRACT_NOT_FOUND("CTR_001", "Contract not found", HttpStatus.NOT_FOUND),
  FORBIDDEN_OPERATION(
      "CTR_002",
      "Only the assigned freelancer can accept or reject this contract",
      HttpStatus.FORBIDDEN),
  INVALID_CONTRACT_STATUS("CTR_003", "Contract is not in PENDING status", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
