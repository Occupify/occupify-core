package com.occupify.core.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.JobSummaryResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectResponseDtoTest {

  @Test
  void testProjectResponseCardAFields() {
    UUID id = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    JobSummaryResponse job =
        new JobSummaryResponse(
            jobId,
            id,
            "Backend Dev",
            "Job Description",
            1,
            SalaryType.MONTHLY,
            BigDecimal.valueOf(1500),
            BigDecimal.valueOf(3000),
            JobStatus.RECRUITING,
            3,
            Set.of("Java"),
            List.of("Req 1"),
            0L,
            LocalDateTime.now(),
            LocalDateTime.now());

    ProjectResponse response =
        new ProjectResponse(
            id,
            "Title",
            "Description",
            ProjectPeriod.MONTHLY,
            LocalDate.now(),
            ExperienceLevel.EXPERT,
            ProjectStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusMonths(3),
            null,
            new EmployerSummaryResponse(
                UUID.randomUUID(), "emp@test.com", "Employer", "avatar.jpg"),
            Set.of("Java", "React"),
            1,
            List.of(job),
            BigDecimal.valueOf(8.0),
            "Hà Nội",
            "3 tháng",
            Set.of("Tag1"),
            0L,
            LocalDateTime.now(),
            LocalDateTime.now());

    assertEquals(Set.of("Java", "React"), response.skills());
    assertEquals(1, response.openJobsCount());
    assertEquals(1, response.jobs().size());
    assertEquals(jobId, response.jobs().get(0).id());
    assertEquals(id, response.jobs().get(0).projectId());
    assertEquals("Backend Dev", response.jobs().get(0).title());
    assertEquals(3, response.jobs().get(0).bidCount());
    assertEquals(BigDecimal.valueOf(1500), response.jobs().get(0).budgetMin());
    assertEquals(BigDecimal.valueOf(3000), response.jobs().get(0).budgetMax());
  }
}
