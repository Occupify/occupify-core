package com.occupify.core.service.impl;

import com.occupify.core.entity.job.Job;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.event.domain.JobStatusChangedDomainEvent;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.service.JobLifecycleService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobLifecycleServiceImpl implements JobLifecycleService {

  private final JobRepository jobRepository;
  private final ContractRepository contractRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void recompute(UUID jobId) {
    if (jobId == null) {
      return;
    }

    Job job =
        jobRepository
            .findForUpdateById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

    long activeContracts =
        contractRepository.countByJobIdAndStatusIn(
            jobId, List.of(ContractStatus.IN_PROGRESS, ContractStatus.COMPLETED));

    JobStatus oldStatus = job.getStatus();
    int quantity = job.getQuantity() != null ? job.getQuantity() : 1;

    if (oldStatus == JobStatus.RECRUITING && activeContracts >= quantity) {
      log.info(
          "[JobLifecycle] Job [{}] transitioning from RECRUITING to FILLED: activeContracts={}, quantity={}",
          jobId,
          activeContracts,
          quantity);
      job.setStatus(JobStatus.FILLED);
      jobRepository.save(job);
      publishStatusChange(job, oldStatus, JobStatus.FILLED);
    } else if (oldStatus == JobStatus.FILLED
        && activeContracts < quantity
        && job.getProject() != null
        && job.getProject().getStatus() == ProjectStatus.OPEN) {
      log.info(
          "[JobLifecycle] Job [{}] transitioning from FILLED to RECRUITING: activeContracts={}, quantity={}",
          jobId,
          activeContracts,
          quantity);
      job.setStatus(JobStatus.RECRUITING);
      jobRepository.save(job);
      publishStatusChange(job, oldStatus, JobStatus.RECRUITING);
    }
  }

  @Override
  @Transactional
  public void closeProjectJobs(UUID projectId) {
    if (projectId == null) {
      return;
    }
    List<Job> jobs = jobRepository.findByProjectId(projectId);
    for (Job job : jobs) {
      if (job.getStatus() == JobStatus.RECRUITING) {
        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
        publishStatusChange(job, oldStatus, JobStatus.CLOSED);
      }
    }
  }

  private void publishStatusChange(Job job, JobStatus oldStatus, JobStatus newStatus) {
    eventPublisher.publishEvent(
        new JobStatusChangedDomainEvent(
            UUID.randomUUID(),
            job.getProject() != null ? job.getProject().getId() : null,
            job.getId(),
            job.getVersion() != null ? job.getVersion() : 0L,
            oldStatus.name(),
            newStatus.name(),
            LocalDateTime.now()));
  }
}
