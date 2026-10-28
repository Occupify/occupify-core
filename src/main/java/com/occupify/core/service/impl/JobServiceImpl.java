package com.occupify.core.service.impl;

import com.occupify.core.dto.request.job.CreateJobRequest;
import com.occupify.core.dto.request.job.UpdateJobRequest;
import com.occupify.core.dto.response.job.JobDetailResponse;
import com.occupify.core.dto.response.job.JobResponse;
import com.occupify.core.entity.bid.Bid;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.exception.project.ProjectErrorCode;
import com.occupify.core.exception.project.ProjectException;
import com.occupify.core.mapper.JobMapper;
import com.occupify.core.repository.bid.BidRepository;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.project.ProjectRepository;
import com.occupify.core.repository.skill.SkillRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.JobService;
import com.occupify.core.service.StorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class JobServiceImpl implements JobService {

  private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(60);

  private final JobRepository jobRepository;
  private final ProjectRepository projectRepository;
  private final BidRepository bidRepository;
  private final SkillRepository skillRepository;
  private final StorageService storageService;
  private final JobMapper jobMapper;

  @Override
  public JobDetailResponse createJob(
      CreateJobRequest request, MultipartFile file, UserContext userContext) {
    log.info("[JobService] Creating job: title={}, user={}", request.title(), userContext.userId());

    if (request.projectId() == null) {
      throw new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND);
    }

    Project project =
        projectRepository
            .findWithEmployerById(request.projectId())
            .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));

    if (!userContext.isAdmin()
        && (project.getEmployer() == null
            || !project.getEmployer().getId().equals(userContext.userId()))) {
      throw new ContractException(ContractErrorCode.JOB_NOT_OWNED);
    }

    String termsFileKey = null;
    if (file != null && !file.isEmpty()) {
      termsFileKey = storageService.uploadPrivateDocument("jobs/" + UUID.randomUUID(), file);
    }

    List<Skill> skills = List.of();
    if (request.skillIds() != null && !request.skillIds().isEmpty()) {
      skills = skillRepository.findAllById(request.skillIds());
    }

    Job job =
        Job.builder()
            .project(project)
            .role(request.role())
            .title(request.title())
            .description(request.description())
            .budget(request.budget())
            .budgetMin(request.budget())
            .budgetMax(request.budget())
            .salaryType(request.salaryType())
            .quantity(1)
            .status(JobStatus.RECRUITING)
            .termsFileKey(termsFileKey)
            .startedAt(request.startedAt())
            .finishedAt(request.finishedAt())
            .skills(new HashSet<>(skills))
            .build();

    Job savedJob = jobRepository.save(job);
    String termsFileUrl = resolveTermsFileUrl(savedJob.getTermsFileKey());
    return jobMapper.toJobDetailResponse(savedJob, 0, termsFileUrl);
  }

  @Override
  @Transactional(readOnly = true)
  public JobDetailResponse getJobDetail(UUID id) {
    Job job =
        jobRepository
            .findWithProjectByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_FOUND));

    int activeBidsCount = countActiveBids(job.getId());
    String termsFileUrl = resolveTermsFileUrl(job.getTermsFileKey());
    return jobMapper.toJobDetailResponse(job, activeBidsCount, termsFileUrl);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<JobResponse> getJobsByProject(UUID projectId, Pageable pageable) {
    Page<Job> jobs = jobRepository.findByProjectIdAndDeletedAtIsNull(projectId, pageable);
    return jobs.map(
        job -> {
          int activeBidsCount = countActiveBids(job.getId());
          String termsFileUrl = resolveTermsFileUrl(job.getTermsFileKey());
          return jobMapper.toJobResponse(job, activeBidsCount, termsFileUrl);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobResponse> getJobsByProject(UUID projectId) {
    List<Job> jobs = jobRepository.findByProjectIdAndDeletedAtIsNull(projectId);
    return jobs.stream()
        .map(
            job -> {
              int activeBidsCount = countActiveBids(job.getId());
              String termsFileUrl = resolveTermsFileUrl(job.getTermsFileKey());
              return jobMapper.toJobResponse(job, activeBidsCount, termsFileUrl);
            })
        .toList();
  }

  @Override
  public JobDetailResponse updateJob(
      UUID id, UpdateJobRequest request, MultipartFile file, UserContext userContext) {
    log.info("[JobService] Updating job: id={}, user={}", id, userContext.userId());

    Job job =
        jobRepository
            .findWithProjectByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_FOUND));

    verifyJobOwnership(job, userContext);

    if (job.getStatus() == JobStatus.CLOSED) {
      throw new ContractException(ContractErrorCode.JOB_ALREADY_CLOSED);
    }

    if (request.role() != null) {
      job.setRole(request.role());
    }
    if (request.title() != null) {
      job.setTitle(request.title());
    }
    if (request.description() != null) {
      job.setDescription(request.description());
    }
    if (request.budget() != null) {
      job.setBudget(request.budget());
      job.setBudgetMin(request.budget());
      job.setBudgetMax(request.budget());
    }
    if (request.salaryType() != null) {
      job.setSalaryType(request.salaryType());
    }
    job.setQuantity(1);
    if (request.startedAt() != null) {
      job.setStartedAt(request.startedAt());
    }
    if (request.finishedAt() != null) {
      job.setFinishedAt(request.finishedAt());
    }
    if (request.skillIds() != null) {
      List<Skill> skills = skillRepository.findAllById(request.skillIds());
      job.setSkills(new HashSet<>(skills));
    }

    if (file != null && !file.isEmpty()) {
      if (job.getTermsFileKey() != null && !job.getTermsFileKey().isBlank()) {
        try {
          storageService.deleteFile(job.getTermsFileKey());
        } catch (Exception ex) {
          log.warn("Failed to delete old terms file key: {}", job.getTermsFileKey(), ex);
        }
      }
      job.setTermsFileKey(storageService.uploadPrivateDocument("jobs/" + job.getId(), file));
    }

    Job savedJob = jobRepository.save(job);
    int activeBidsCount = countActiveBids(savedJob.getId());
    String termsFileUrl = resolveTermsFileUrl(savedJob.getTermsFileKey());
    return jobMapper.toJobDetailResponse(savedJob, activeBidsCount, termsFileUrl);
  }

  @Override
  public JobDetailResponse closeJob(UUID id, UserContext userContext) {
    log.info("[JobService] Closing job from project: id={}, user={}", id, userContext.userId());

    Job job =
        jobRepository
            .findWithProjectByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_FOUND));

    verifyJobOwnership(job, userContext);

    if (job.getStatus() == JobStatus.CLOSED) {
      throw new ContractException(ContractErrorCode.JOB_ALREADY_CLOSED);
    }

    job.setStatus(JobStatus.CLOSED);

    // Reject all pending bids for this job
    rejectPendingBids(job.getId());

    Job savedJob = jobRepository.save(job);
    String termsFileUrl = resolveTermsFileUrl(savedJob.getTermsFileKey());
    return jobMapper.toJobDetailResponse(savedJob, 0, termsFileUrl);
  }

  @Override
  public void deleteJob(UUID id, UserContext userContext) {
    log.info("[JobService] Soft deleting job: id={}, user={}", id, userContext.userId());

    Job job =
        jobRepository
            .findWithProjectByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_FOUND));

    verifyJobOwnership(job, userContext);

    job.setDeletedAt(LocalDateTime.now());
    job.setStatus(JobStatus.CLOSED);

    rejectPendingBids(job.getId());

    jobRepository.save(job);
  }

  private void verifyJobOwnership(Job job, UserContext userContext) {
    if (userContext.isAdmin()) {
      return;
    }
    if (job.getProject() == null
        || job.getProject().getEmployer() == null
        || !job.getProject().getEmployer().getId().equals(userContext.userId())) {
      throw new ContractException(ContractErrorCode.JOB_NOT_OWNED);
    }
  }

  private void rejectPendingBids(UUID jobId) {
    List<Bid> bids = bidRepository.findByJobId(jobId);
    for (Bid bid : bids) {
      if (bid.getStatus() == BidStatus.PENDING) {
        bid.setStatus(BidStatus.REJECTED);
        bidRepository.save(bid);
      }
    }
  }

  private int countActiveBids(UUID jobId) {
    List<Bid> bids = bidRepository.findByJobId(jobId);
    return (int) bids.stream().filter(b -> b.getStatus() == BidStatus.PENDING).count();
  }

  private String resolveTermsFileUrl(String termsFileKey) {
    if (termsFileKey == null || termsFileKey.isBlank()) {
      return null;
    }
    return storageService.generatePresignedDownloadUrl(termsFileKey, PRESIGNED_URL_DURATION);
  }
}
