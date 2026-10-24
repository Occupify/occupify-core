package com.occupify.core.entity.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectTest {

  @Test
  void testProjectBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    User employer = User.builder().email("emp@test.com").build();
    LocalDate dueDate = LocalDate.of(2026, 12, 31);
    LocalDateTime closedAt = LocalDateTime.now();
    LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 9, 0);
    LocalDateTime endDate = LocalDateTime.of(2026, 6, 1, 18, 0);

    Project project =
        Project.builder()
            .employer(employer)
            .title("Website Redesign")
            .description("Redesign company website")
            .startPrice(BigDecimal.valueOf(2500.00))
            .period(ProjectPeriod.DAY)
            .dueDate(dueDate)
            .experienceLevel(ExperienceLevel.EXPERT)
            .status(ProjectStatus.OPEN)
            .startDate(startDate)
            .endDate(endDate)
            .closedAt(closedAt)
            .build();
    project.setId(id);

    assertEquals(id, project.getId());
    assertEquals(employer, project.getEmployer());
    assertEquals("Website Redesign", project.getTitle());
    assertEquals("Redesign company website", project.getDescription());
    assertEquals(BigDecimal.valueOf(2500.00), project.getStartPrice());
    assertEquals(ProjectPeriod.DAY, project.getPeriod());
    assertEquals(ExperienceLevel.EXPERT, project.getExperienceLevel());
    assertEquals(ProjectStatus.OPEN, project.getStatus());
    assertEquals(startDate, project.getStartDate());
    assertEquals(endDate, project.getEndDate());
    assertEquals(closedAt, project.getClosedAt());
  }
}
