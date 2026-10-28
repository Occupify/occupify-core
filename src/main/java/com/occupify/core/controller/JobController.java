package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.base.SuccessResponse;
import com.occupify.core.dto.request.job.CreateJobRequest;
import com.occupify.core.dto.request.job.UpdateJobRequest;
import com.occupify.core.dto.response.job.JobDetailResponse;
import com.occupify.core.dto.response.job.JobResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "Jobs", description = "Endpoints for creating, listing, updating, and closing project jobs")
public class JobController extends AbstractBaseController {

  private final JobService jobService;

  @PostMapping(value = RouteConstants.JOBS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Create a job", description = "Creates a new job under a project with optional terms file")
  public ResponseEntity<CreatedResponse<JobDetailResponse>> createJob(
      @RequestPart("data") @Valid CreateJobRequest request,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    JobDetailResponse response = jobService.createJob(request, file, userContext);
    return created(response, MessageConstants.JOB_CREATED);
  }

  @PostMapping(value = RouteConstants.PROJECTS + "/{projectId}" + RouteConstants.JOBS, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create a job under project", description = "Creates a new job explicitly under specified project")
  public ResponseEntity<CreatedResponse<JobDetailResponse>> createJobUnderProject(
      @PathVariable UUID projectId,
      @Valid @RequestBody CreateJobRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    CreateJobRequest effectiveRequest =
        new CreateJobRequest(
            projectId,
            request.role(),
            request.title(),
            request.description(),
            request.budget(),
            request.salaryType(),
            request.startedAt(),
            request.finishedAt(),
            request.skillIds());
    JobDetailResponse response = jobService.createJob(effectiveRequest, null, userContext);
    return created(response, MessageConstants.JOB_CREATED);
  }

  @GetMapping(RouteConstants.PROJECTS + "/{projectId}" + RouteConstants.JOBS)
  @Operation(summary = "Get jobs by project", description = "Retrieves paginated list of jobs under a given project")
  public ResponseEntity<PageResponse<JobResponse>> getJobsByProject(
      @PathVariable UUID projectId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<JobResponse> page = jobService.getJobsByProject(projectId, pageable);
    return paging(page, MessageConstants.JOB_LIST_RETRIEVED);
  }

  @GetMapping(RouteConstants.JOBS + RouteConstants.JOB_ID)
  @Operation(summary = "Get job details", description = "Retrieves full details of a specific job by ID")
  public ResponseEntity<SingleResponse<JobDetailResponse>> getJobDetail(@PathVariable UUID id) {
    JobDetailResponse response = jobService.getJobDetail(id);
    return successSingle(response, MessageConstants.JOB_RETRIEVED);
  }

  @PutMapping(value = RouteConstants.JOBS + RouteConstants.JOB_ID, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Update a job", description = "Updates job details and optional terms file")
  public ResponseEntity<SingleResponse<JobDetailResponse>> updateJob(
      @PathVariable UUID id,
      @RequestPart("data") @Valid UpdateJobRequest request,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    JobDetailResponse response = jobService.updateJob(id, request, file, userContext);
    return successSingle(response, MessageConstants.JOB_UPDATED);
  }

  @PatchMapping(RouteConstants.JOBS + RouteConstants.JOB_CLOSE)
  @Operation(
      summary = "Close job from project",
      description = "Closes a job from the project when not yet a contract and rejects pending bids")
  public ResponseEntity<SingleResponse<JobDetailResponse>> closeJob(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    JobDetailResponse response = jobService.closeJob(id, userContext);
    return successSingle(response, MessageConstants.JOB_CLOSED);
  }

  @DeleteMapping(RouteConstants.JOBS + RouteConstants.JOB_ID)
  @Operation(summary = "Delete job", description = "Soft deletes a job and cancels pending bids")
  public ResponseEntity<SuccessResponse> deleteJob(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    jobService.deleteJob(id, userContext);
    return success(MessageConstants.JOB_DELETED);
  }
}
