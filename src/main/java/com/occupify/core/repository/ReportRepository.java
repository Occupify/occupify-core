package com.occupify.core.repository;

import com.occupify.core.entity.Report;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, UUID> {
  List<Report> findByReporterId(UUID reporterId);

  List<Report> findByTargetUserId(UUID targetUserId);

  List<Report> findByTargetProjectId(UUID targetProjectId);
}
