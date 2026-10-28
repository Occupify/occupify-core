package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.occupify.core.dto.request.job.CreateJobRequest;
import com.occupify.core.dto.request.job.UpdateJobRequest;
import com.occupify.core.dto.response.job.JobDetailResponse;
import com.occupify.core.entity.bid.Bid;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import com.occupify.core.enums.UserRole;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.JobMapper;
import com.occupify.core.repository.bid.BidRepository;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.project.ProjectRepository;
import com.occupify.core.repository.skill.SkillRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.impl.JobServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JobServiceTest {

  private JobRepository jobRepository;
  private ProjectRepository projectRepository;
  private BidRepository bidRepository;
  private SkillRepository skillRepository;
  private StorageService storageService;
  private JobMapper jobMapper;
  private JobServiceImpl jobService;

  private UUID employerId;
  private UUID projectId;
  private User employer;
  private Project project;
  private UserContext employerContext;

  @BeforeEach
  void setUp() {
    jobRepository = mock(JobRepository.class);
    projectRepository = mock(ProjectRepository.class);
    bidRepository = mock(BidRepository.class);
    skillRepository = mock(SkillRepository.class);
    storageService = mock(StorageService.class);
    jobMapper = mock(JobMapper.class);

    jobService =
        new JobServiceImpl(
            jobRepository,
            projectRepository,
            bidRepository,
            skillRepository,
            storageService,
            jobMapper);

    employerId = UUID.randomUUID();
    projectId = UUID.randomUUID();

    employer = User.builder().id(employerId).email("emp@occupify.local").fullName("Employer").build();
    project = Project.builder().employer(employer).title("Project Title").build();
    project.setId(projectId);
    employerContext = new UserContext(employerId, UserRole.USER, "emp@occupify.local");
  }

  @Test
  @DisplayName("createJob should create job with status RECRUITING")
  void createJob_Success() {
    CreateJobRequest request =
        CreateJobRequest.builder()
            .projectId(projectId)
            .role("Backend Developer")
            .title("Spring Boot Specialist")
            .description("Need an expert")
            .budget(BigDecimal.valueOf(25000000))
            .salaryType(SalaryType.MONTHLY)
            .build();

    when(projectRepository.findWithEmployerById(projectId)).thenReturn(Optional.of(project));
    when(jobRepository.save(any(Job.class)))
        .thenAnswer(inv -> {
          Job j = inv.getArgument(0);
          j.setId(UUID.randomUUID());
          return j;
        });

    JobDetailResponse expectedResponse =
        JobDetailResponse.builder()
            .id(UUID.randomUUID())
            .projectId(projectId)
            .role("Backend Developer")
            .title("Spring Boot Specialist")
            .status(JobStatus.RECRUITING)
            .build();
    when(jobMapper.toJobDetailResponse(any(Job.class), eq(0), any())).thenReturn(expectedResponse);

    JobDetailResponse response = jobService.createJob(request, null, employerContext);

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(JobStatus.RECRUITING);

    ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).save(captor.capture());
    Job saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(JobStatus.RECRUITING);
    assertThat(saved.getRole()).isEqualTo("Backend Developer");
    assertThat(saved.getBudget()).isEqualByComparingTo(BigDecimal.valueOf(25000000));
  }

  @Test
  @DisplayName("closeJob should set status CLOSED and reject pending bids")
  void closeJob_Success() {
    UUID jobId = UUID.randomUUID();
    Job job =
        Job.builder()
            .project(project)
            .role("Frontend Dev")
            .title("React Dev")
            .status(JobStatus.RECRUITING)
            .build();
    job.setId(jobId);

    Bid pendingBid = Bid.builder().job(job).status(BidStatus.PENDING).build();
    pendingBid.setId(UUID.randomUUID());
    Bid acceptedBid = Bid.builder().job(job).status(BidStatus.ACCEPTED).build();
    acceptedBid.setId(UUID.randomUUID());

    when(jobRepository.findWithProjectByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
    when(bidRepository.findByJobId(jobId)).thenReturn(List.of(pendingBid, acceptedBid));
    when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

    JobDetailResponse closedResponse =
        JobDetailResponse.builder().id(jobId).status(JobStatus.CLOSED).build();
    when(jobMapper.toJobDetailResponse(any(Job.class), eq(0), any())).thenReturn(closedResponse);

    JobDetailResponse result = jobService.closeJob(jobId, employerContext);

    assertThat(result.status()).isEqualTo(JobStatus.CLOSED);
    assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    assertThat(pendingBid.getStatus()).isEqualTo(BidStatus.REJECTED);
    assertThat(acceptedBid.getStatus()).isEqualTo(BidStatus.ACCEPTED); // not altered

    verify(bidRepository).save(pendingBid);
    verify(jobRepository).save(job);
  }

  @Test
  @DisplayName("deleteJob should soft delete and close job")
  void deleteJob_Success() {
    UUID jobId = UUID.randomUUID();
    Job job =
        Job.builder()
            .project(project)
            .role("Mobile Dev")
            .title("Flutter Dev")
            .status(JobStatus.RECRUITING)
            .build();
    job.setId(jobId);

    when(jobRepository.findWithProjectByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
    when(bidRepository.findByJobId(jobId)).thenReturn(List.of());

    jobService.deleteJob(jobId, employerContext);

    assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
    assertThat(job.getDeletedAt()).isNotNull();
    verify(jobRepository).save(job);
  }
}
