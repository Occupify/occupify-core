package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.request.RejectContractRequest;
import com.occupify.core.dto.response.ContractResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(RouteConstants.CONTRACTS)
@Tag(name = "Contracts", description = "Endpoints for managing contract acceptance and rejection")
public class ContractController extends AbstractBaseController {

  private final ContractService contractService;

  @GetMapping(RouteConstants.CONTRACT_ID)
  @Operation(summary = "Get contract details", description = "Retrieves contract by ID")
  public ResponseEntity<SingleResponse<ContractResponse>> getContract(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ContractResponse response = contractService.getContract(id, userContext);
    return successSingle(response, MessageConstants.CONTRACT_RETRIEVED);
  }

  @PutMapping(RouteConstants.CONTRACT_ACCEPT)
  @Operation(summary = "Accept contract", description = "Freelancer accepts a pending contract")
  public ResponseEntity<SingleResponse<ContractResponse>> acceptContract(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ContractResponse response = contractService.acceptContract(id, userContext);
    return accepted(response, MessageConstants.CONTRACT_ACCEPTED);
  }

  @PutMapping(RouteConstants.CONTRACT_REJECT)
  @Operation(
      summary = "Reject contract",
      description = "Freelancer rejects a pending contract offer")
  public ResponseEntity<SingleResponse<ContractResponse>> rejectContract(
      @PathVariable UUID id,
      @RequestBody(required = false) RejectContractRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ContractResponse response = contractService.rejectContract(id, request, userContext);
    return successSingle(response, MessageConstants.CONTRACT_REJECTED);
  }
}
