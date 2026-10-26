package com.occupify.core.dto.request.contract;

import com.occupify.core.enums.ContractStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ContractFilterRequest(UUID projectId, UUID jobId, ContractStatus status) {}
