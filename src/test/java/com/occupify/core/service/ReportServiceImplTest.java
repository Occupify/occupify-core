package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.dto.request.report.CreateReportRequest;
import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.entity.outbox.OutboxEvent;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.report.Report;
import com.occupify.core.entity.report.ReportEvidence;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ReportReasonCategory;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.enums.ReportTargetType;
import com.occupify.core.enums.UserRole;
import com.occupify.core.exception.report.ReportErrorCode;
import com.occupify.core.exception.report.ReportException;
import com.occupify.core.mapper.ReportMapper;
import com.occupify.core.repository.outbox.OutboxEventRepository;
import com.occupify.core.repository.project.ProjectRepository;
import com.occupify.core.repository.report.ReportEvidenceRepository;
import com.occupify.core.repository.report.ReportRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.scheduler.OutboxRelayScheduler;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.impl.ReportServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ReportServiceImplTest {

  private UserRepository userRepository;
  private ProjectRepository projectRepository;
  private ReportRepository reportRepository;
  private ReportEvidenceRepository reportEvidenceRepository;
  private OutboxEventRepository outboxEventRepository;
  private StorageService storageService;
  private ObjectMapper objectMapper;
  private OutboxRelayScheduler outboxRelayScheduler;
  private ReportMapper reportMapper;
  private ReportServiceImpl reportService;

  private UUID reporterId;
  private UUID targetUserId;
  private UUID targetProjectId;
  private User reporter;
  private User targetUser;
  private Project targetProject;
  private UserContext userContext;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    projectRepository = mock(ProjectRepository.class);
    reportRepository = mock(ReportRepository.class);
    reportEvidenceRepository = mock(ReportEvidenceRepository.class);
    outboxEventRepository = mock(OutboxEventRepository.class);
    storageService = mock(StorageService.class);
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    outboxRelayScheduler = mock(OutboxRelayScheduler.class);
    reportMapper = new ReportMapper();

    reportService =
        new ReportServiceImpl(
            userRepository,
            projectRepository,
            reportRepository,
            reportEvidenceRepository,
            outboxEventRepository,
            storageService,
            objectMapper,
            outboxRelayScheduler,
            reportMapper);

    reporterId = UUID.randomUUID();
    targetUserId = UUID.randomUUID();
    targetProjectId = UUID.randomUUID();

    reporter = User.builder().id(reporterId).email("reporter@occupify.local").build();
    targetUser = User.builder().id(targetUserId).email("target@occupify.local").build();
    targetProject = Project.builder().title("Test Project").build();
    targetProject.setId(targetProjectId);

    userContext = new UserContext(reporterId, UserRole.USER, "reporter@occupify.local");
  }

  @Test
  @DisplayName("Should successfully create report on user with evidence files")
  void createReport_OnUserWithFiles_Success() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .reasonCategory(ReportReasonCategory.SPAM)
            .description("Spamming fake jobs")
            .build();

    MockMultipartFile file =
        new MockMultipartFile("files", "proof.png", "image/png", "dummy".getBytes());
    List<MultipartFile> files = List.of(file);

    when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
    when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
    when(storageService.uploadDocument(eq("reports"), any(MultipartFile.class)))
        .thenReturn("https://storage.occupify.com/reports/proof.png");

    UUID savedReportId = UUID.randomUUID();
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(
            invocation -> {
              Report r = invocation.getArgument(0);
              r.setId(savedReportId);
              r.setCreatedAt(LocalDateTime.now());
              return r;
            });
    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ReportResponse response = reportService.createReport(request, files, userContext);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(savedReportId);
    assertThat(response.reporterId()).isEqualTo(reporterId);
    assertThat(response.targetType()).isEqualTo(ReportTargetType.USER);
    assertThat(response.targetUserId()).isEqualTo(targetUserId);
    assertThat(response.reasonCategory()).isEqualTo(ReportReasonCategory.SPAM);
    assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
    assertThat(response.evidenceUrls())
        .containsExactly("https://storage.occupify.com/reports/proof.png");

    verify(reportEvidenceRepository).save(any(ReportEvidence.class));
    verify(outboxEventRepository).save(any(OutboxEvent.class));
    verify(outboxRelayScheduler).relayDirectly(any(OutboxEvent.class));
  }

  @Test
  @DisplayName("Should successfully create report on project without files")
  void createReport_OnProject_Success() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.PROJECT)
            .targetProjectId(targetProjectId)
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("Fraudulent payment terms")
            .build();

    when(projectRepository.findById(targetProjectId)).thenReturn(Optional.of(targetProject));
    when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));

    UUID savedReportId = UUID.randomUUID();
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(
            invocation -> {
              Report r = invocation.getArgument(0);
              r.setId(savedReportId);
              r.setCreatedAt(LocalDateTime.now());
              return r;
            });
    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ReportResponse response = reportService.createReport(request, null, userContext);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(savedReportId);
    assertThat(response.targetProjectId()).isEqualTo(targetProjectId);
    assertThat(response.evidenceUrls()).isEmpty();

    verify(reportEvidenceRepository, never()).save(any(ReportEvidence.class));
    verify(outboxEventRepository).save(any(OutboxEvent.class));
    verify(outboxRelayScheduler).relayDirectly(any(OutboxEvent.class));
  }

  @Test
  @DisplayName("Should throw CANNOT_REPORT_SELF when reporter reports self")
  void createReport_SelfReport_ThrowsException() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(reporterId)
            .reasonCategory(ReportReasonCategory.HARASSMENT)
            .description("Reporting myself")
            .build();

    assertThatThrownBy(() -> reportService.createReport(request, null, userContext))
        .isInstanceOf(ReportException.class)
        .hasFieldOrPropertyWithValue("code", ReportErrorCode.CANNOT_REPORT_SELF.code());
  }

  @Test
  @DisplayName("Should throw TARGET_USER_REQUIRED when targetType is USER but targetUserId is null")
  void createReport_UserTargetMissingId_ThrowsException() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(null)
            .reasonCategory(ReportReasonCategory.HARASSMENT)
            .description("Missing target user ID")
            .build();

    assertThatThrownBy(() -> reportService.createReport(request, null, userContext))
        .isInstanceOf(ReportException.class)
        .hasFieldOrPropertyWithValue("code", ReportErrorCode.TARGET_USER_REQUIRED.code());
  }

  @Test
  @DisplayName(
      "Should throw TARGET_PROJECT_REQUIRED when targetType is PROJECT but targetProjectId is null")
  void createReport_ProjectTargetMissingId_ThrowsException() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.PROJECT)
            .targetProjectId(null)
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("Missing target project ID")
            .build();

    assertThatThrownBy(() -> reportService.createReport(request, null, userContext))
        .isInstanceOf(ReportException.class)
        .hasFieldOrPropertyWithValue("code", ReportErrorCode.TARGET_PROJECT_REQUIRED.code());
  }

  @Test
  @DisplayName("Should throw TARGET_USER_NOT_FOUND when target user does not exist")
  void createReport_TargetUserNotFound_ThrowsException() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .reasonCategory(ReportReasonCategory.SPAM)
            .description("User does not exist")
            .build();

    when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reportService.createReport(request, null, userContext))
        .isInstanceOf(ReportException.class)
        .hasFieldOrPropertyWithValue("code", ReportErrorCode.TARGET_USER_NOT_FOUND.code());
  }

  @Test
  @DisplayName("Should throw TARGET_PROJECT_NOT_FOUND when target project does not exist")
  void createReport_TargetProjectNotFound_ThrowsException() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.PROJECT)
            .targetProjectId(targetProjectId)
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("Project does not exist")
            .build();

    when(projectRepository.findById(targetProjectId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reportService.createReport(request, null, userContext))
        .isInstanceOf(ReportException.class)
        .hasFieldOrPropertyWithValue("code", ReportErrorCode.TARGET_PROJECT_NOT_FOUND.code());
  }

  @Test
  @DisplayName("Should throw REPORTER_NOT_FOUND when reporter is not in database")
  void createReport_ReporterNotFound_ThrowsException() {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .reasonCategory(ReportReasonCategory.SPAM)
            .description("Reporter not in DB")
            .build();

    when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
    when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reportService.createReport(request, null, userContext))
        .isInstanceOf(ReportException.class)
        .hasFieldOrPropertyWithValue("code", ReportErrorCode.REPORTER_NOT_FOUND.code());
  }
}
