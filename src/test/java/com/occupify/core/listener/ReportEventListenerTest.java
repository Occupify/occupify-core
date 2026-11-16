package com.occupify.core.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.entity.report.Report;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.event.ReportUpdatedEvent;
import com.occupify.core.repository.report.ReportRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportEventListenerTest {

  private ReportRepository reportRepository;
  private ReportEventListener reportEventListener;

  @BeforeEach
  void setUp() {
    reportRepository = mock(ReportRepository.class);
    reportEventListener = new ReportEventListener(reportRepository);
  }

  @Test
  @DisplayName("Should successfully update report status on valid ReportUpdatedEvent")
  void onReportUpdated_Success() {
    UUID reportId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();

    Report report = Report.builder().status(ReportStatus.PENDING).build();
    report.setId(reportId);

    ReportUpdatedEvent event =
        ReportUpdatedEvent.builder()
            .reportId(reportId)
            .status("RESOLVED")
            .moderatedByAdminId(adminId)
            .note("Action taken on reported user")
            .updatedAt(Instant.now())
            .build();

    when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

    reportEventListener.onReportUpdated(event);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    verify(reportRepository).save(report);
  }

  @Test
  @DisplayName("Should skip update when report is not found")
  void onReportUpdated_ReportNotFound() {
    UUID reportId = UUID.randomUUID();

    ReportUpdatedEvent event =
        ReportUpdatedEvent.builder()
            .reportId(reportId)
            .status("DISMISSED")
            .moderatedByAdminId(UUID.randomUUID())
            .note("No violation found")
            .updatedAt(Instant.now())
            .build();

    when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

    reportEventListener.onReportUpdated(event);

    verify(reportRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should handle null or invalid event gracefully")
  void onReportUpdated_InvalidEvent() {
    reportEventListener.onReportUpdated(null);
    verify(reportRepository, never()).findById(any());

    ReportUpdatedEvent nullReportIdEvent =
        ReportUpdatedEvent.builder().reportId(null).status("RESOLVED").build();
    reportEventListener.onReportUpdated(nullReportIdEvent);
    verify(reportRepository, never()).findById(any());
  }

  @Test
  @DisplayName("Should handle invalid status string gracefully")
  void onReportUpdated_InvalidStatus() {
    UUID reportId = UUID.randomUUID();
    Report report = Report.builder().status(ReportStatus.PENDING).build();
    report.setId(reportId);

    ReportUpdatedEvent event =
        ReportUpdatedEvent.builder()
            .reportId(reportId)
            .status("NON_EXISTENT_STATUS")
            .updatedAt(Instant.now())
            .build();

    when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

    reportEventListener.onReportUpdated(event);

    assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    verify(reportRepository, never()).save(report);
  }
}
