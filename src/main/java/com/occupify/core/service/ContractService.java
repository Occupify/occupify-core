package com.occupify.core.service;

import com.occupify.core.dto.request.RejectContractRequest;
import com.occupify.core.dto.response.ContractResponse;
import com.occupify.core.security.UserContext;
import java.util.UUID;

public interface ContractService {

  ContractResponse acceptContract(UUID contractId, UserContext userContext);

  ContractResponse rejectContract(
      UUID contractId, RejectContractRequest request, UserContext userContext);

  ContractResponse getContract(UUID contractId, UserContext userContext);

  void confirmContractEscrow(UUID contractId);

  void compensateContract(UUID contractId, String reason);
}
