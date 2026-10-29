package com.occupify.core.mapper;

import com.occupify.core.dto.response.job.JobDetailResponse;
import com.occupify.core.dto.response.job.JobResponse;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.skill.Skill;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface JobMapper {

  @Mapping(target = "id", source = "job.id")
  @Mapping(target = "projectId", source = "job.project.id")
  @Mapping(target = "projectTitle", source = "job.project.title")
  @Mapping(target = "role", source = "job.role")
  @Mapping(target = "title", source = "job.title")
  @Mapping(target = "description", source = "job.description")
  @Mapping(target = "budget", source = "job.budget")
  @Mapping(target = "salaryType", source = "job.salaryType")
  @Mapping(target = "termsFileUrl", source = "termsFileUrl")
  @Mapping(target = "startedAt", source = "job.startedAt")
  @Mapping(target = "finishedAt", source = "job.finishedAt")
  @Mapping(target = "status", source = "job.status")
  @Mapping(target = "quantity", source = "job.quantity")
  @Mapping(target = "activeBidsCount", source = "activeBidsCount")
  @Mapping(target = "skills", source = "job.skills", qualifiedByName = "mapSkills")
  @Mapping(target = "createdAt", source = "job.createdAt")
  @Mapping(target = "updatedAt", source = "job.updatedAt")
  JobDetailResponse toJobDetailResponse(Job job, Integer activeBidsCount, String termsFileUrl);

  @Mapping(target = "id", source = "job.id")
  @Mapping(target = "projectId", source = "job.project.id")
  @Mapping(target = "role", source = "job.role")
  @Mapping(target = "title", source = "job.title")
  @Mapping(target = "description", source = "job.description")
  @Mapping(target = "budget", source = "job.budget")
  @Mapping(target = "salaryType", source = "job.salaryType")
  @Mapping(target = "termsFileUrl", source = "termsFileUrl")
  @Mapping(target = "status", source = "job.status")
  @Mapping(target = "quantity", source = "job.quantity")
  @Mapping(target = "activeBidsCount", source = "activeBidsCount")
  @Mapping(target = "skills", source = "job.skills", qualifiedByName = "mapSkills")
  @Mapping(target = "startedAt", source = "job.startedAt")
  @Mapping(target = "finishedAt", source = "job.finishedAt")
  @Mapping(target = "createdAt", source = "job.createdAt")
  JobResponse toJobResponse(Job job, Integer activeBidsCount, String termsFileUrl);

  @Named("mapSkills")
  default Set<String> mapSkills(Set<Skill> skills) {
    if (skills == null) {
      return Set.of();
    }
    return skills.stream().map(Skill::getName).collect(Collectors.toSet());
  }
}
