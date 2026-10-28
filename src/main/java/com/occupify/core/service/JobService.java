package com.occupify.core.service;

import com.occupify.core.dto.request.job.CreateJobRequest;
import com.occupify.core.dto.request.job.UpdateJobRequest;
import com.occupify.core.dto.response.job.JobDetailResponse;
import com.occupify.core.dto.response.job.JobResponse;
import com.occupify.core.security.UserContext;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface JobService {

  JobDetailResponse createJob(
      CreateJobRequest request, MultipartFile file, UserContext userContext);

  JobDetailResponse getJobDetail(UUID id);

  Page<JobResponse> getJobsByProject(UUID projectId, Pageable pageable);

  List<JobResponse> getJobsByProject(UUID projectId);

  JobDetailResponse updateJob(
      UUID id, UpdateJobRequest request, MultipartFile file, UserContext userContext);

  JobDetailResponse closeJob(UUID id, UserContext userContext);

  void deleteJob(UUID id, UserContext userContext);
}
