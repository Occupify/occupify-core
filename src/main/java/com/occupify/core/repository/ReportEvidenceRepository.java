package com.occupify.core.repository;

import com.occupify.core.entity.ReportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportEvidenceRepository extends JpaRepository<ReportEvidence, UUID> {
    List<ReportEvidence> findByReportId(UUID reportId);
}
