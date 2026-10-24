package com.occupify.core.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectResponseDtoTest {

  @Test
  void testProjectResponseCardAFields() {
    UUID id = UUID.randomUUID();
    ProjectResponse response =
        new ProjectResponse(
            id,
            "Title",
            "Description",
            BigDecimal.valueOf(1000),
            ProjectPeriod.MONTHLY,
            LocalDate.now(),
            ExperienceLevel.EXPERT,
            ProjectStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusMonths(3),
            null,
            new EmployerSummaryResponse(
                UUID.randomUUID(), "emp@test.com", "Employer", "avatar.jpg"),
            BigDecimal.valueOf(1500),
            BigDecimal.valueOf(3000),
            Set.of("Java", "React"),
            2,
            LocalDateTime.now(),
            LocalDateTime.now());

    assertEquals(BigDecimal.valueOf(1500), response.salaryMin());
    assertEquals(BigDecimal.valueOf(3000), response.salaryMax());
    assertEquals(Set.of("Java", "React"), response.skills());
    assertEquals(2, response.openJobsCount());
  }
}
