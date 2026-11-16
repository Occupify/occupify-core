package com.occupify.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.dto.request.report.CreateReportRequest;
import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.entity.outbox.OutboxEvent;
import com.occupify.core.entity.outbox.OutboxStatus;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.report.Report;
import com.occupify.core.entity.report.ReportEvidence;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.enums.ReportTargetType;
import com.occupify.core.event.ReportCreatedEvent;
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
import com.occupify.core.service.ReportService;
import com.occupify.core.service.StorageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ReportRepository reportRepository;
  private final ReportEvidenceRepository reportEvidenceRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final StorageService storageService;
  private final ObjectMapper objectMapper;
  private final OutboxRelayScheduler outboxRelayScheduler;
  private final ReportMapper reportMapper;

  @Override
  @Transactional
  public ReportResponse createReport(
      CreateReportRequest request, List<MultipartFile> files, UserContext userContext) {
    log.info(
        "[ReportService] Creating report by reporter [{}] on targetType [{}]",
        userContext.getUserId(),
        request.targetType());

    // 1. Validate reporter cannot self-report
    if (userContext.getUserId() != null && userContext.getUserId().equals(request.targetUserId())) {
      log.warn("[ReportService] Reporter [{}] attempted to self-report", userContext.getUserId());
      throw new ReportException(ReportErrorCode.CANNOT_REPORT_SELF);
    }

    // 2. Validate target type specific requirements
    if (request.targetType() == ReportTargetType.USER && request.targetUserId() == null) {
      log.warn(
          "[ReportService] Report creation failed: targetUserId is required for targetType USER");
      throw new ReportException(ReportErrorCode.TARGET_USER_REQUIRED);
    }

    if (request.targetType() == ReportTargetType.PROJECT && request.targetProjectId() == null) {
      log.warn(
          "[ReportService] Report creation failed: targetProjectId is required for targetType PROJECT");
      throw new ReportException(ReportErrorCode.TARGET_PROJECT_REQUIRED);
    }

    // 3. Verify target user exists if targetUserId provided
    User targetUser = null;
    if (request.targetUserId() != null) {
      targetUser =
          userRepository
              .findById(request.targetUserId())
              .orElseThrow(
                  () -> {
                    log.warn("[ReportService] Target user [{}] not found", request.targetUserId());
                    return new ReportException(ReportErrorCode.TARGET_USER_NOT_FOUND);
                  });
    }

    // 4. Verify target project exists if targetProjectId provided
    Project targetProject = null;
    if (request.targetProjectId() != null) {
      targetProject =
          projectRepository
              .findById(request.targetProjectId())
              .orElseThrow(
                  () -> {
                    log.warn(
                        "[ReportService] Target project [{}] not found", request.targetProjectId());
                    return new ReportException(ReportErrorCode.TARGET_PROJECT_NOT_FOUND);
                  });
    }

    // 5. Look up reporter user
    User reporter =
        userRepository
            .findById(userContext.getUserId())
            .orElseThrow(
                () -> {
                  log.error(
                      "[ReportService] Reporter user [{}] not found in database",
                      userContext.getUserId());
                  return new ReportException(ReportErrorCode.REPORTER_NOT_FOUND);
                });

    // 6. Upload evidence files
    List<String> uploadedUrls = new ArrayList<>();
    if (files != null && !files.isEmpty()) {
      for (MultipartFile file : files) {
        if (file != null && !file.isEmpty()) {
          log.debug(
              "[ReportService] Uploading evidence file [{}] of size [{}] bytes",
              file.getOriginalFilename(),
              file.getSize());
          String url = storageService.uploadDocument("reports", file);
          uploadedUrls.add(url);
        }
      }
    }

    // 7. Persist Report entity
    Report report =
        Report.builder()
            .reporter(reporter)
            .targetType(request.targetType())
            .targetUser(targetUser)
            .targetProject(targetProject)
            .reasonCategory(request.reasonCategory())
            .description(request.description())
            .status(ReportStatus.PENDING)
            .build();

    Report savedReport = reportRepository.save(report);
    log.info(
        "[ReportService] Successfully persisted report [{}] with status [{}]",
        savedReport.getId(),
        savedReport.getStatus());

    // 8. Persist ReportEvidence records
    if (!uploadedUrls.isEmpty()) {
      for (String url : uploadedUrls) {
        ReportEvidence evidence = ReportEvidence.builder().report(savedReport).fileUrl(url).build();
        reportEvidenceRepository.save(evidence);
      }
      log.debug(
          "[ReportService] Persisted [{}] evidence record(s) for report [{}]",
          uploadedUrls.size(),
          savedReport.getId());
    }

    // 9. Build and serialize ReportCreatedEvent
    ReportCreatedEvent event =
        ReportCreatedEvent.builder()
            .reportId(savedReport.getId())
            .reporterUserId(reporter.getId())
            .targetType(savedReport.getTargetType().name())
            .targetUserId(
                savedReport.getTargetUser() != null ? savedReport.getTargetUser().getId() : null)
            .targetProjectId(
                savedReport.getTargetProject() != null
                    ? savedReport.getTargetProject().getId()
                    : null)
            .reasonCategory(savedReport.getReasonCategory().name())
            .description(savedReport.getDescription())
            .evidenceUrls(uploadedUrls)
            .status(savedReport.getStatus().name())
            .createdAt(Instant.now())
            .build();

    String payloadJson;
    try {
      payloadJson = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error(
          "[ReportService] Failed to serialize ReportCreatedEvent for report [{}]: {}",
          savedReport.getId(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to serialize ReportCreatedEvent", e);
    }

    // 10. Persist OutboxEvent and trigger immediate relay
    OutboxEvent outboxEvent =
        OutboxEvent.builder()
            .aggregateType("REPORT")
            .aggregateId(savedReport.getId())
            .eventType("REPORT_CREATED")
            .routingKey("report.created")
            .payload(payloadJson)
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();

    final OutboxEvent finalOutboxEvent = outboxEventRepository.save(outboxEvent);
    log.debug(
        "[ReportService] Saved outbox event [{}] for report [{}]",
        finalOutboxEvent.getId(),
        savedReport.getId());

    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager
          .registerSynchronization(
              new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                  outboxRelayScheduler.relayDirectly(finalOutboxEvent);
                }
              });
    } else {
      outboxRelayScheduler.relayDirectly(finalOutboxEvent);
    }

    return reportMapper.toResponseWithUrls(savedReport, uploadedUrls);
  }
}
