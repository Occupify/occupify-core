package com.occupify.core.entity;

import com.occupify.core.enums.ReportReasonCategory;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.enums.ReportTargetType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportTest {

    @Test
    void testReportBuilderAndGetters() {
        UUID id = UUID.randomUUID();
        User reporter = User.builder().email("rep@test.com").build();
        User targetUser = User.builder().email("target@test.com").build();
        Project targetProject = Project.builder().title("Proj").build();

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.USER)
                .targetUser(targetUser)
                .targetProject(targetProject)
                .reasonCategory(ReportReasonCategory.SPAM_HARASSMENT)
                .description("Spam messages sent")
                .status(ReportStatus.PENDING)
                .build();
        report.setId(id);

        assertEquals(id, report.getId());
        assertEquals(reporter, report.getReporter());
        assertEquals(ReportTargetType.USER, report.getTargetType());
        assertEquals(targetUser, report.getTargetUser());
        assertEquals(targetProject, report.getTargetProject());
        assertEquals(ReportReasonCategory.SPAM_HARASSMENT, report.getReasonCategory());
        assertEquals("Spam messages sent", report.getDescription());
        assertEquals(ReportStatus.PENDING, report.getStatus());
    }
}
