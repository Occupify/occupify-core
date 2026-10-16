package com.occupify.core.service.impl;

import com.occupify.core.dto.request.CreateProjectRequest;
import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.dto.request.UpdateProjectRequest;
import com.occupify.core.dto.response.JobSummaryResponse;
import com.occupify.core.dto.response.ProjectDetailResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.event.ProjectClosedEvent;
import com.occupify.core.event.ProjectCreatedEvent;
import com.occupify.core.exception.project.ProjectErrorCode;
import com.occupify.core.exception.project.ProjectException;
import com.occupify.core.exception.system.SystemErrorCode;
import com.occupify.core.exception.system.SystemException;
import com.occupify.core.mapper.ProjectMapper;
import com.occupify.core.producer.ProjectEventProducer;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.project.ProjectRepository;
import com.occupify.core.repository.project.ProjectSpecification;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ProjectService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

  private final ProjectRepository projectRepository;
  private final JobRepository jobRepository;
  private final UserRepository userRepository;
  private final ProjectMapper projectMapper;
  private final ProjectEventProducer projectEventProducer;

  @Override
  @Transactional
  public ProjectResponse createProject(CreateProjectRequest request, UserContext userContext) {
    log.info(
        "[ProjectService] Creating project '{}' by user [{}]",
        request.title(),
        userContext.userId());

    if (!userContext.isUser()) {
      throw new SystemException(
          SystemErrorCode.FORBIDDEN, "Only users are permitted to create projects");
    }

    User employer =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(
                () ->
                    new ProjectException(
                        ProjectErrorCode.EMPLOYER_NOT_FOUND,
                        "Employer not found with ID: " + userContext.userId()));

    Project project = projectMapper.toEntity(request);
    project.setEmployer(employer);

    Project saved = projectRepository.save(project);

    projectEventProducer.publishProjectCreated(
        new ProjectCreatedEvent(
            saved.getId(),
            saved.getTitle(),
            employer.getId(),
            employer.getEmail(),
            saved.getCreatedAt()));

    return projectMapper.toResponse(saved);
  }

  @Override
  public ProjectDetailResponse getProjectDetail(UUID id) {
    log.info("[ProjectService] Retrieving project details for ID [{}]", id);
    Project project =
        projectRepository
            .findWithEmployerById(id)
            .orElseThrow(
                () ->
                    new ProjectException(
                        ProjectErrorCode.PROJECT_NOT_FOUND, "Project not found with ID: " + id));

    List<Job> jobs = jobRepository.findWithSkillsByProjectId(id);
    List<JobSummaryResponse> jobSummaries = projectMapper.toJobSummaries(jobs);

    return projectMapper.toDetailResponse(project, jobSummaries);
  }

  @Override
  public Page<ProjectResponse> getProjects(ProjectFilterRequest filter, Pageable pageable) {
    log.info("[ProjectService] Querying projects with filters: {}", filter);
    Specification<Project> spec = ProjectSpecification.withFilters(filter);
    Page<Project> page = projectRepository.findAll(spec, pageable);
    return page.map(projectMapper::toResponse);
  }

  @Override
  @Transactional
  public ProjectResponse updateProject(
      UUID id, UpdateProjectRequest request, UserContext userContext) {
    log.info("[ProjectService] Updating project [{}] by user [{}]", id, userContext.userId());
    Project project =
        projectRepository
            .findWithEmployerById(id)
            .orElseThrow(
                () ->
                    new ProjectException(
                        ProjectErrorCode.PROJECT_NOT_FOUND, "Project not found with ID: " + id));

    checkProjectOwner(project, userContext);

    projectMapper.updateEntity(project, request);
    Project updated = projectRepository.save(project);

    return projectMapper.toResponse(updated);
  }

  @Override
  @Transactional
  public ProjectResponse closeProject(UUID id, UserContext userContext) {
    log.info("[ProjectService] Closing project [{}] by user [{}]", id, userContext.userId());
    Project project =
        projectRepository
            .findWithEmployerById(id)
            .orElseThrow(
                () ->
                    new ProjectException(
                        ProjectErrorCode.PROJECT_NOT_FOUND, "Project not found with ID: " + id));

    checkProjectClosePermission(project, userContext);

    if (project.getStatus() == ProjectStatus.CLOSED) {
      throw new ProjectException(
          ProjectErrorCode.PROJECT_ALREADY_CLOSED, "Project " + id + " is already closed");
    }

    project.setStatus(ProjectStatus.CLOSED);
    project.setClosedAt(LocalDateTime.now());
    Project closed = projectRepository.save(project);

    projectEventProducer.publishProjectClosed(
        new ProjectClosedEvent(
            closed.getId(),
            closed.getTitle(),
            closed.getEmployer().getId(),
            closed.getEmployer().getEmail(),
            closed.getClosedAt()));

    return projectMapper.toResponse(closed);
  }

  private void checkProjectOwner(Project project, UserContext userContext) {
    if (!userContext.isUser() || !project.getEmployer().getId().equals(userContext.userId())) {
      throw new ProjectException(
          ProjectErrorCode.FORBIDDEN_OPERATION,
          "Only the project owner is permitted to update project " + project.getId());
    }
  }

  private void checkProjectClosePermission(Project project, UserContext userContext) {
    if (!project.getEmployer().getId().equals(userContext.userId()) && !userContext.isAdmin()) {
      throw new ProjectException(
          ProjectErrorCode.FORBIDDEN_OPERATION,
          "You do not have permission to close project " + project.getId());
    }
  }
}
