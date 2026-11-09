package com.occupify.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.report.Report;
import com.occupify.core.entity.report.ReportEvidence;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ReportReasonCategory;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.enums.ReportTargetType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportMapperTest {

  private final ReportMapper mapper = new ReportMapper();

  @Test
  @DisplayName("Should map Report entity and ReportEvidence list to ReportResponse")
  void toResponse_WithEvidences() {
    UUID reportId = UUID.randomUUID();
    UUID reporterId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    User reporter = User.builder().id(reporterId).build();
    User targetUser = User.builder().id(targetUserId).build();

    Report report =
        Report.builder()
            .reporter(reporter)
            .targetType(ReportTargetType.USER)
            .targetUser(targetUser)
            .reasonCategory(ReportReasonCategory.HARASSMENT)
            .description("Offensive remarks")
            .status(ReportStatus.PENDING)
            .build();
    report.setId(reportId);
    report.setCreatedAt(now);

    ReportEvidence evidence =
        ReportEvidence.builder()
            .report(report)
            .fileUrl("https://occupify.com/evidence1.png")
            .build();

    ReportResponse response = mapper.toResponse(report, List.of(evidence));

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(reportId);
    assertThat(response.reporterId()).isEqualTo(reporterId);
    assertThat(response.targetType()).isEqualTo(ReportTargetType.USER);
    assertThat(response.targetUserId()).isEqualTo(targetUserId);
    assertThat(response.reasonCategory()).isEqualTo(ReportReasonCategory.HARASSMENT);
    assertThat(response.description()).isEqualTo("Offensive remarks");
    assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
    assertThat(response.evidenceUrls()).containsExactly("https://occupify.com/evidence1.png");
    assertThat(response.createdAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should map Report entity for project target with empty evidences")
  void toResponse_ProjectTarget() {
    UUID reportId = UUID.randomUUID();
    UUID reporterId = UUID.randomUUID();
    UUID targetProjectId = UUID.randomUUID();

    User reporter = new User();
    reporter.setId(reporterId);
    Project project = Project.builder().build();
    project.setId(targetProjectId);

    Report report =
        Report.builder()
            .reporter(reporter)
            .targetType(ReportTargetType.PROJECT)
            .targetProject(project)
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("Fake project")
            .status(ReportStatus.RESOLVED)
            .build();
    report.setId(reportId);

    ReportResponse response = mapper.toResponse(report, null);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(reportId);
    assertThat(response.targetProjectId()).isEqualTo(targetProjectId);
    assertThat(response.targetUserId()).isNull();
    assertThat(response.evidenceUrls()).isEmpty();
  }

  @Test
  @DisplayName("Should return null when Report entity is null")
  void toResponse_NullReport() {
    assertThat(mapper.toResponse(null, List.of())).isNull();
    assertThat(mapper.toResponseWithUrls(null, List.of())).isNull();
  }
}
