package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.base.SuccessResponse;
import com.occupify.core.dto.request.contract.CompleteContractRequest;
import com.occupify.core.dto.request.contract.ContractFilterRequest;
import com.occupify.core.dto.request.contract.CreateContractRequest;
import com.occupify.core.dto.request.contract.UpdateContractRequest;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(RouteConstants.CONTRACTS)
@Tag(name = "Contracts", description = "Endpoints for creating, managing, accepting, completing, and canceling contracts")
public class ContractController extends AbstractBaseController {

    private final ContractService contractService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a contract", description = "Creates a new contract for an applicant or external freelancer with optional terms file")
    public ResponseEntity<CreatedResponse<ContractDetailResponse>> createContract(
            @RequestPart("data") @Valid CreateContractRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.createContract(request, file, userContext);
        return created(response, MessageConstants.CONTRACT_CREATED);
    }

    @PutMapping(value = RouteConstants.CONTRACT_ID, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a contract", description = "Updates contract details and optionally replaces the terms file (PENDING only)")
    public ResponseEntity<SingleResponse<ContractDetailResponse>> updateContract(
            @PathVariable UUID id,
            @RequestPart("data") @Valid UpdateContractRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.updateContract(id, request, file, userContext);
        return successSingle(response, MessageConstants.CONTRACT_UPDATED);
    }

    @GetMapping
    @Operation(summary = "Get paginated contracts", description = "Retrieves a paginated list of contracts for the current authenticated user")
    public ResponseEntity<PageResponse<ContractSummaryResponse>> getContracts(
            @ParameterObject ContractFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) @Authenticated UserContext userContext) {
        Page<ContractSummaryResponse> page = contractService.getContracts(filter, pageable, userContext);
        return paging(page, MessageConstants.CONTRACT_LIST_RETRIEVED);
    }

    @GetMapping(RouteConstants.CONTRACT_ID)
    @Operation(summary = "Get contract detail", description = "Retrieves full details of a specific contract by ID")
    public ResponseEntity<SingleResponse<ContractDetailResponse>> getContractDetail(
            @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.getContractDetail(id, userContext);
        return successSingle(response, MessageConstants.CONTRACT_RETRIEVED);
    }

    @PostMapping(RouteConstants.CONTRACT_CANCEL)
    @Operation(summary = "Cancel a contract", description = "Cancels a contract by ID")
    public ResponseEntity<SingleResponse<ContractDetailResponse>> cancelContract(
            @PathVariable UUID id,
            @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.cancelContract(id, userContext);
        return successSingle(response, MessageConstants.CONTRACT_CANCELLED);
    }

    @PostMapping(RouteConstants.CONTRACT_ACCEPT)
    @Operation(summary = "Accept a contract", description = "Accepts a pending contract by the freelancer/employee")
    public ResponseEntity<SingleResponse<ContractDetailResponse>> acceptContract(
            @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.acceptContract(id, userContext);
        return accepted(response, MessageConstants.CONTRACT_ACCEPTED);
    }

    @PostMapping(RouteConstants.CONTRACT_REJECT)
    @Operation(summary = "Reject a contract", description = "Rejects a pending contract offer by the freelancer/employee")
    public ResponseEntity<SingleResponse<ContractDetailResponse>> rejectContract(
            @PathVariable UUID id,
            @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.rejectContract(id, userContext);
        return successSingle(response, MessageConstants.CONTRACT_REJECTED);
    }

    @PostMapping(RouteConstants.CONTRACT_COMPLETE)
    @Operation(summary = "Complete a contract", description = "Completes an in-progress contract with rating and review by the employer")
    public ResponseEntity<SingleResponse<ContractDetailResponse>> completeContract(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteContractRequest request,
            @Parameter(hidden = true) @Authenticated UserContext userContext) {
        ContractDetailResponse response = contractService.completeContract(id, request, userContext);
        return successSingle(response, MessageConstants.CONTRACT_COMPLETED);
    }

    @DeleteMapping(RouteConstants.CONTRACT_ID)
    @Operation(summary = "Delete a contract", description = "Soft deletes a contract and rejects pending proposals")
    public ResponseEntity<SuccessResponse> deleteContract(
            @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
        contractService.removeContract(id, userContext);
        return success("Remove contract successfully");
    }
}
