package com.occupify.core.mapper;

import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.entity.report.Report;
import com.occupify.core.entity.report.ReportEvidence;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

  public ReportResponse toResponse(Report report, List<ReportEvidence> evidences) {
    if (report == null) {
      return null;
    }

    List<String> evidenceUrls =
        evidences == null
            ? Collections.emptyList()
            : evidences.stream().map(ReportEvidence::getFileUrl).toList();

    return toResponseWithUrls(report, evidenceUrls);
  }

  public ReportResponse toResponseWithUrls(Report report, List<String> evidenceUrls) {
    if (report == null) {
      return null;
    }

    return ReportResponse.builder()
        .id(report.getId())
        .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
        .targetType(report.getTargetType())
        .targetUserId(report.getTargetUser() != null ? report.getTargetUser().getId() : null)
        .targetProjectId(
            report.getTargetProject() != null ? report.getTargetProject().getId() : null)
        .reasonCategory(report.getReasonCategory())
        .description(report.getDescription())
        .status(report.getStatus())
        .evidenceUrls(evidenceUrls != null ? evidenceUrls : Collections.emptyList())
        .createdAt(report.getCreatedAt())
        .build();
  }
}
