package com.occupify.core.repository.report;

import com.occupify.core.entity.report.ReportEvidence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportEvidenceRepository extends JpaRepository<ReportEvidence, UUID> {
  List<ReportEvidence> findByReportId(UUID reportId);
}
