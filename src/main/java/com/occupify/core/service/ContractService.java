package com.occupify.core.service;

import com.occupify.core.dto.request.contract.CompleteContractRequest;
import com.occupify.core.dto.request.contract.ContractFilterRequest;
import com.occupify.core.dto.request.contract.CreateContractRequest;
import com.occupify.core.dto.request.contract.UpdateContractRequest;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.security.UserContext;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ContractService {

  ContractDetailResponse createContract(
      CreateContractRequest request, MultipartFile file, UserContext userContext);

  ContractDetailResponse updateContract(
      UUID id, UpdateContractRequest request, MultipartFile file, UserContext userContext);

  Page<ContractSummaryResponse> getContracts(
      ContractFilterRequest filter, Pageable pageable, UserContext userContext);

  ContractDetailResponse getContractDetail(UUID id, UserContext userContext);

  ContractDetailResponse cancelContract(UUID id, UserContext userContext);

  ContractDetailResponse acceptContract(UUID id, UserContext userContext);

  ContractDetailResponse rejectContract(UUID id, UserContext userContext);

  ContractDetailResponse completeContract(
      UUID id, CompleteContractRequest request, UserContext userContext);

  void removeContract(UUID id, UserContext userContext);

  void confirmContractEscrow(UUID contractId);

  void compensateContract(UUID contractId, String reason);
}
