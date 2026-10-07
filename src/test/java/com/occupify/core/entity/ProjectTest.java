package com.occupify.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    Project project =
        Project.builder()
            .employer(employer)
            .title("Website Redesign")
            .description("Redesign company website")
            .startPrice(BigDecimal.valueOf(2500.00))
            .period(ProjectPeriod.FIXED)
            .dueDate(dueDate)
            .experienceLevel(ExperienceLevel.EXPERT)
            .status(ProjectStatus.OPEN)
            .closedAt(closedAt)
            .build();
    project.setId(id);

    assertEquals(id, project.getId());
    assertEquals(employer, project.getEmployer());
    assertEquals("Website Redesign", project.getTitle());
    assertEquals("Redesign company website", project.getDescription());
    assertEquals(BigDecimal.valueOf(2500.00), project.getStartPrice());
    assertEquals(ProjectPeriod.FIXED, project.getPeriod());
    assertEquals(dueDate, project.getDueDate());
    assertEquals(ExperienceLevel.EXPERT, project.getExperienceLevel());
    assertEquals(ProjectStatus.OPEN, project.getStatus());
    assertEquals(closedAt, project.getClosedAt());
  }
}
