package com.occupify.core.mapper;

import com.occupify.core.dto.response.agent.AgentJobResponse;
import com.occupify.core.dto.response.agent.AgentProjectResponse;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.enums.JobStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AgentProjectMapper {

  private static final Comparator<Job> JOB_COMPARATOR =
      Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Job::getId, Comparator.nullsLast(Comparator.naturalOrder()));

  public AgentProjectResponse toResponse(
      Project project, List<Job> jobs, Map<UUID, Integer> bidCounts) {
    if (project == null) {
      return null;
    }

    List<Job> safeJobs = jobs != null ? jobs : List.of();
    Map<UUID, Integer> safeBidCounts = bidCounts != null ? bidCounts : Map.of();

    List<AgentJobResponse> mappedJobs =
        safeJobs.stream()
            .sorted(JOB_COMPARATOR)
            .map(job -> toJobResponse(job, safeBidCounts.getOrDefault(job.getId(), 0)))
            .toList();

    List<String> sortedTags =
        project.getTags() != null
            ? project.getTags().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()
            : List.of();

    List<String> unionSkills =
        safeJobs.stream()
            .map(Job::getSkills)
            .filter(Objects::nonNull)
            .flatMap(skills -> skills.stream().map(Skill::getName))
            .filter(Objects::nonNull)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();

    int openJobsCount =
        (int) safeJobs.stream().filter(j -> j.getStatus() == JobStatus.RECRUITING).count();

    return new AgentProjectResponse(
        project.getId(),
        project.getVersion() != null ? project.getVersion() : 0L,
        project.getTitle(),
        project.getDescription(),
        project.getStartPrice(),
        project.getPeriod(),
        project.getDueDate(),
        project.getExperienceLevel(),
        project.getStatus(),
        project.getStartDate(),
        project.getEndDate(),
        project.getClosedAt(),
        project.getHoursPerDay(),
        project.getLocation(),
        project.getDuration(),
        sortedTags,
        unionSkills,
        openJobsCount,
        mappedJobs,
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  public AgentJobResponse toJobResponse(Job job, int bidCount) {
    if (job == null) {
      return null;
    }

    List<String> sortedSkills =
        job.getSkills() != null
            ? job.getSkills().stream()
                .map(Skill::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList()
            : List.of();

    List<String> storedRequirements =
        job.getRequirements() != null ? new ArrayList<>(job.getRequirements()) : List.of();

    return new AgentJobResponse(
        job.getId(),
        job.getVersion() != null ? job.getVersion() : 0L,
        job.getProject() != null ? job.getProject().getId() : null,
        job.getTitle(),
        job.getDescription(),
        job.getQuantity(),
        job.getSalaryType(),
        job.getBudgetMin(),
        job.getBudgetMax(),
        job.getStatus(),
        bidCount,
        sortedSkills,
        storedRequirements,
        job.getCreatedAt(),
        job.getUpdatedAt());
  }
}
