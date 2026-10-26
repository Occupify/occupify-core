package com.occupify.core.mapper;

import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.JobSummaryResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.dto.response.contract.CancellationContractResponse;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.entity.contract.CancellationContract;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.entity.user.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContractMapper {

  @Mapping(target = "employerId", source = "employer.id")
  @Mapping(target = "employerName", source = "employer.fullName")
  @Mapping(target = "employeeId", source = "employee.id")
  @Mapping(target = "employeeName", source = "employee.fullName")
  @Mapping(target = "jobId", source = "job.id")
  @Mapping(target = "jobTitle", source = "job.title")
  @Mapping(target = "projectId", source = "job.project.id")
  @Mapping(target = "projectTitle", source = "job.project.title")
  ContractSummaryResponse toSummaryResponse(Contract contract);

  @Mapping(target = "id", source = "contract.id")
  @Mapping(target = "title", source = "contract.title")
  @Mapping(target = "employer", source = "contract.employer")
  @Mapping(target = "employee", source = "contract.employee")
  @Mapping(target = "job", source = "contract.job")
  @Mapping(target = "project", source = "contract.job.project")
  @Mapping(target = "sourceType", source = "contract.sourceType")
  @Mapping(target = "sourceId", source = "contract.sourceId")
  @Mapping(target = "agreedRate", source = "contract.agreedRate")
  @Mapping(target = "salaryType", source = "contract.salaryType")
  @Mapping(target = "salaryDueDate", source = "contract.salaryDueDate")
  @Mapping(target = "jobDescription", source = "contract.jobDescription")
  @Mapping(target = "status", source = "contract.status")
  @Mapping(target = "paymentStatus", source = "contract.paymentStatus")
  @Mapping(target = "termsFileUrl", source = "termsFileUrl")
  @Mapping(target = "cancellation", source = "cancellation")
  @Mapping(target = "startedAt", source = "contract.startedAt")
  @Mapping(target = "finishedAt", source = "contract.finishedAt")
  @Mapping(target = "createdAt", source = "contract.createdAt")
  @Mapping(target = "updatedAt", source = "contract.updatedAt")
  ContractDetailResponse toDetailResponse(
      Contract contract, String termsFileUrl, CancellationContractResponse cancellation);

  @Mapping(target = "contractId", source = "cancellation.contract.id")
  @Mapping(target = "requestedByUserId", source = "cancellation.requestedByUser.id")
  @Mapping(target = "requestedByUserName", source = "cancellation.requestedByUser.fullName")
  @Mapping(target = "evidenceFileUrl", source = "evidenceFileUrl")
  CancellationContractResponse toCancellationResponse(
      CancellationContract cancellation, String evidenceFileUrl);

  EmployerSummaryResponse toEmployerSummary(User user);

  @Mapping(target = "skills", source = "skills", qualifiedByName = "mapSkills")
  @Mapping(target = "projectId", source = "project.id")
  JobSummaryResponse toJobSummary(Job job);

  @Mapping(target = "employer", source = "employer")
  @Mapping(target = "skills", ignore = true)
  @Mapping(target = "openJobsCount", ignore = true)
  @Mapping(target = "jobs", ignore = true)
  ProjectResponse toProjectResponse(Project project);

  @Named("mapSkills")
  default Set<String> mapSkills(Set<Skill> skills) {
    if (skills == null) {
      return Set.of();
    }
    return skills.stream().map(Skill::getName).collect(Collectors.toSet());
  }
}
