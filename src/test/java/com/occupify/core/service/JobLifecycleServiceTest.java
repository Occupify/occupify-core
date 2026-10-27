package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.service.impl.JobLifecycleServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class JobLifecycleServiceTest {

  @Mock private JobRepository jobRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private JobLifecycleServiceImpl lifecycleService;

  @BeforeEach
  void setUp() {
    lifecycleService =
        new JobLifecycleServiceImpl(jobRepository, contractRepository, eventPublisher);
  }

  @Test
  void recompute_whenActiveContractsEqualQuantity_fillsJob() {
    UUID jobId = UUID.randomUUID();
    Job job =
        Job.builder()
            .quantity(2)
            .status(JobStatus.RECRUITING)
            .version(0L)
            .build();
    job.setId(jobId);

    when(jobRepository.findForUpdateById(jobId)).thenReturn(Optional.of(job));
    when(contractRepository.countByJobIdAndStatusIn(eq(jobId), any())).thenReturn(2L);

    lifecycleService.recompute(jobId);

    assertThat(job.getStatus()).isEqualTo(JobStatus.FILLED);
    verify(jobRepository).save(job);
    verify(eventPublisher).publishEvent(any(JobLifecycleServiceImpl.JobStatusChangeEvent.class));
  }

  @Test
  void recompute_whenFilledJobLosesContract_reopensToRecruiting() {
    UUID jobId = UUID.randomUUID();
    Project project = Project.builder().status(ProjectStatus.OPEN).build();
    Job job =
        Job.builder()
            .project(project)
            .quantity(2)
            .status(JobStatus.FILLED)
            .version(1L)
            .build();
    job.setId(jobId);

    when(jobRepository.findForUpdateById(jobId)).thenReturn(Optional.of(job));
    when(contractRepository.countByJobIdAndStatusIn(eq(jobId), any())).thenReturn(1L);

    lifecycleService.recompute(jobId);

    assertThat(job.getStatus()).isEqualTo(JobStatus.RECRUITING);
    verify(jobRepository).save(job);
    verify(eventPublisher).publishEvent(any(JobLifecycleServiceImpl.JobStatusChangeEvent.class));
  }

  @Test
  void closeProjectJobs_closesOnlyRecruitingJobs() {
    UUID projectId = UUID.randomUUID();
    Job recruitingJob =
        Job.builder().status(JobStatus.RECRUITING).version(0L).build();
    recruitingJob.setId(UUID.randomUUID());

    Job filledJob =
        Job.builder().status(JobStatus.FILLED).version(0L).build();
    filledJob.setId(UUID.randomUUID());

    when(jobRepository.findByProjectId(projectId))
        .thenReturn(List.of(recruitingJob, filledJob));

    lifecycleService.closeProjectJobs(projectId);

    assertThat(recruitingJob.getStatus()).isEqualTo(JobStatus.CLOSED);
    assertThat(filledJob.getStatus()).isEqualTo(JobStatus.FILLED);

    verify(jobRepository).save(recruitingJob);
    verify(jobRepository, never()).save(filledJob);
  }
}
