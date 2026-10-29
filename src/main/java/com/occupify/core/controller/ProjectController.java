package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.request.CreateProjectRequest;
import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.dto.request.UpdateProjectRequest;
import com.occupify.core.dto.response.ProjectDetailResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(RouteConstants.PROJECTS)
@Tag(
    name = "Projects",
    description = "Endpoints for creating, querying, updating, and closing projects")
public class ProjectController extends AbstractBaseController {

  private final ProjectService projectService;

  @PostMapping
  @Operation(
      summary = "Create a new project",
      description = "Creates a new project owned by the authenticated user. Authorized for users.")
  public ResponseEntity<CreatedResponse<ProjectResponse>> createProject(
      @Valid @RequestBody CreateProjectRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ProjectResponse response = projectService.createProject(request, userContext);
    return created(response, MessageConstants.PROJECT_CREATED);
  }

  @GetMapping
  @Operation(
      summary = "Get paginated projects",
      description =
          "Retrieves a paginated list of projects with optional keyword and metadata filters. Excludes searching user's own projects when authenticated. (Public)")
  public ResponseEntity<PageResponse<ProjectResponse>> getProjects(
      @ParameterObject ProjectFilterRequest filter,
      @Parameter(
              description =
                  "Optional authenticated caller ID to exclude owned projects from search results")
          @RequestHeader(value = "X-User-Id", required = false)
          UUID headerUserId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    ProjectFilterRequest effectiveFilter = filter;
    UUID excludeId =
        filter != null && filter.excludeEmployerId() != null
            ? filter.excludeEmployerId()
            : headerUserId;

    if (excludeId != null && (filter == null || filter.employerId() == null)) {
      effectiveFilter =
          new ProjectFilterRequest(
              filter != null ? filter.keyword() : null,
              filter != null ? filter.status() : null,
              filter != null ? filter.employerId() : null,
              filter != null ? filter.period() : null,
              filter != null ? filter.experienceLevel() : null,
              filter != null ? filter.minPrice() : null,
              filter != null ? filter.maxPrice() : null,
              filter != null ? filter.skills() : null,
              excludeId);
    }

    Page<ProjectResponse> page = projectService.getProjects(effectiveFilter, pageable);
    return paging(page, MessageConstants.PROJECT_LIST_RETRIEVED);
  }

  @GetMapping(RouteConstants.PROJECT_ID)
  @Operation(
      summary = "Get project details",
      description =
          "Retrieves complete project details including associated jobs and skills (Public)")
  public ResponseEntity<SingleResponse<ProjectDetailResponse>> getProjectDetail(
      @PathVariable UUID id) {
    ProjectDetailResponse response = projectService.getProjectDetail(id);
    return successSingle(response, MessageConstants.PROJECT_RETRIEVED);
  }

  @PutMapping(RouteConstants.PROJECT_ID)
  @Operation(
      summary = "Update an existing project",
      description =
          "Updates project details. Authorized for users (only permitted by project owner)")
  public ResponseEntity<SingleResponse<ProjectResponse>> updateProject(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProjectRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ProjectResponse response = projectService.updateProject(id, request, userContext);
    return successSingle(response, MessageConstants.PROJECT_UPDATED);
  }

  @PatchMapping(RouteConstants.PROJECT_CLOSE)
  @Operation(
      summary = "Close a project",
      description =
          "Marks project as CLOSED and publishes project.closed RabbitMQ event. Allowed by users (owner) and admins")
  public ResponseEntity<SingleResponse<ProjectResponse>> closeProject(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ProjectResponse response = projectService.closeProject(id, userContext);
    return successSingle(response, MessageConstants.PROJECT_CLOSED);
  }
}
