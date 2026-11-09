package com.occupify.core.listener;

import com.occupify.core.entity.report.Report;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.event.ReportUpdatedEvent;
import com.occupify.core.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportEventListener {

  private final ReportRepository reportRepository;

  @RabbitListener(queues = "${app.rabbitmq.report-core-queue:occupify.core.report.queue}")
  @Transactional
  public void onReportUpdated(ReportUpdatedEvent event) {
    if (event == null || event.reportId() == null || event.status() == null) {
      log.warn("[ReportEventListener] Received invalid ReportUpdatedEvent: {}", event);
      return;
    }

    log.info(
        "[ReportEventListener] Processing ReportUpdatedEvent for reportId [{}], status [{}], moderatedByAdminId [{}]",
        event.reportId(),
        event.status(),
        event.moderatedByAdminId());

    Report report = reportRepository.findById(event.reportId()).orElse(null);
    if (report == null) {
      log.warn(
          "[ReportEventListener] Report with ID [{}] not found. Skipping status update.",
          event.reportId());
      return;
    }

    try {
      ReportStatus oldStatus = report.getStatus();
      ReportStatus newStatus = ReportStatus.valueOf(event.status().trim().toUpperCase());
      report.setStatus(newStatus);
      reportRepository.save(report);
      log.info(
          "[ReportEventListener] Successfully updated report [{}] status from [{}] to [{}] (admin note: {})",
          report.getId(),
          oldStatus,
          newStatus,
          event.note());
    } catch (IllegalArgumentException e) {
      log.error(
          "[ReportEventListener] Invalid status [{}] received for report [{}]: {}",
          event.status(),
          event.reportId(),
          e.getMessage());
    }
  }
}
