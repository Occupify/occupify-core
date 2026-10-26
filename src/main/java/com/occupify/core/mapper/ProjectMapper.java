package com.occupify.core.mapper;

import com.occupify.core.dto.request.CreateProjectRequest;
import com.occupify.core.dto.request.UpdateProjectRequest;
import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.JobSummaryResponse;
import com.occupify.core.dto.response.ProjectDetailResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.entity.user.User;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProjectMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "employer", ignore = true)
  @Mapping(target = "status", constant = "OPEN")
  @Mapping(target = "closedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Project toEntity(CreateProjectRequest request);

  @Mapping(target = "employer", source = "employer")
  @Mapping(target = "skills", ignore = true)
  @Mapping(target = "openJobsCount", ignore = true)
  @Mapping(target = "jobs", ignore = true)
  ProjectResponse toResponse(Project project);

  @Mapping(target = "employer", source = "project.employer")
  @Mapping(target = "jobs", source = "jobs")
  @Mapping(target = "id", source = "project.id")
  @Mapping(target = "title", source = "project.title")
  @Mapping(target = "description", source = "project.description")
  @Mapping(target = "startPrice", source = "project.startPrice")
  @Mapping(target = "period", source = "project.period")
  @Mapping(target = "dueDate", source = "project.dueDate")
  @Mapping(target = "experienceLevel", source = "project.experienceLevel")
  @Mapping(target = "status", source = "project.status")
  @Mapping(target = "startDate", source = "project.startDate")
  @Mapping(target = "endDate", source = "project.endDate")
  @Mapping(target = "closedAt", source = "project.closedAt")
  @Mapping(target = "createdAt", source = "project.createdAt")
  @Mapping(target = "updatedAt", source = "project.updatedAt")
  ProjectDetailResponse toDetailResponse(Project project, List<JobSummaryResponse> jobs);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "employer", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "closedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(@MappingTarget Project project, UpdateProjectRequest request);

  EmployerSummaryResponse toEmployerSummary(User employer);

  @Mapping(target = "skills", source = "job.skills", qualifiedByName = "mapSkills")
  @Mapping(target = "projectId", source = "job.project.id")
  @Mapping(target = "bidCount", source = "bidCount")
  @Mapping(target = "id", source = "job.id")
  @Mapping(target = "title", source = "job.title")
  @Mapping(target = "description", source = "job.description")
  @Mapping(target = "quantity", source = "job.quantity")
  @Mapping(target = "salaryType", source = "job.salaryType")
  @Mapping(target = "budgetMin", source = "job.budgetMin")
  @Mapping(target = "budgetMax", source = "job.budgetMax")
  @Mapping(target = "status", source = "job.status")
  @Mapping(target = "createdAt", source = "job.createdAt")
  @Mapping(target = "updatedAt", source = "job.updatedAt")
  JobSummaryResponse toJobSummary(Job job, Integer bidCount);

  default JobSummaryResponse toJobSummary(Job job) {
    return toJobSummary(job, 0);
  }

  default List<JobSummaryResponse> toJobSummaries(List<Job> jobs) {
    if (jobs == null) {
      return List.of();
    }
    return jobs.stream().map(this::toJobSummary).toList();
  }

  @Named("mapSkills")
  default Set<String> mapSkills(Set<Skill> skills) {
    if (skills == null) {
      return Set.of();
    }
    return skills.stream().map(Skill::getName).collect(Collectors.toSet());
  }
}
