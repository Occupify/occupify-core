package com.occupify.core.mapper;

import com.occupify.core.dto.response.ContractResponse;
import com.occupify.core.entity.contract.Contract;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContractMapper {

  @Mapping(target = "jobId", source = "contract.job.id")
  @Mapping(target = "projectId", source = "contract.job.project.id")
  @Mapping(target = "employerId", source = "contract.job.project.employer.id")
  @Mapping(target = "freelancerId", source = "contract.freelancer.id")
  ContractResponse toResponse(Contract contract);
}
