package com.occupify.core.service;

import com.occupify.core.dto.request.CreateProjectRequest;
import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.dto.request.UpdateProjectRequest;
import com.occupify.core.dto.response.ProjectDetailResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.security.UserContext;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

  ProjectResponse createProject(CreateProjectRequest request, UserContext userContext);

  ProjectDetailResponse getProjectDetail(UUID id);

  Page<ProjectResponse> getProjects(ProjectFilterRequest filter, Pageable pageable);

  Page<ProjectDetailResponse> getProjectsWithDetails(
      ProjectFilterRequest filter, Pageable pageable);

  ProjectResponse updateProject(UUID id, UpdateProjectRequest request, UserContext userContext);

  ProjectResponse closeProject(UUID id, UserContext userContext);
}
