package com.occupify.core.mapper;

import com.occupify.core.dto.request.CreateBidRequest;
import com.occupify.core.dto.request.UpdateBidRequest;
import com.occupify.core.dto.response.BidJobSummaryResponse;
import com.occupify.core.dto.response.BidProjectSummaryResponse;
import com.occupify.core.dto.response.BidResponse;
import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.FreelancerSummaryResponse;
import com.occupify.core.entity.bid.Bid;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface BidMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "job", ignore = true)
  @Mapping(target = "freelancer", ignore = true)
  @Mapping(target = "attachmentCvUrl", ignore = true)
  @Mapping(target = "status", constant = "PENDING")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Bid toEntity(CreateBidRequest request);

  @Mapping(target = "job", source = "job")
  @Mapping(target = "project", source = "job.project")
  @Mapping(target = "freelancer", source = "freelancer")
  BidResponse toResponse(Bid bid);

  BidJobSummaryResponse toJobSummary(Job job);

  @Mapping(target = "employer", source = "employer")
  BidProjectSummaryResponse toProjectSummary(Project project);

  FreelancerSummaryResponse toFreelancerSummary(User freelancer);

  EmployerSummaryResponse toEmployerSummary(User employer);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "job", ignore = true)
  @Mapping(target = "freelancer", ignore = true)
  @Mapping(target = "attachmentCvUrl", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(@MappingTarget Bid bid, UpdateBidRequest request);
}
