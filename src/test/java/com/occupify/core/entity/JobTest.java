package com.occupify.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobTest {

  @Test
  void testJobBuilderAndDefaults() {
    Job defaultJob = Job.builder().build();
    assertEquals(1, defaultJob.getQuantity());
    assertTrue(defaultJob.getSkills().isEmpty());

    UUID id = UUID.randomUUID();
    Project project = Project.builder().title("Proj").build();
    Skill skill = Skill.builder().name("React").build();

    Job job =
        Job.builder()
            .project(project)
            .title("Frontend Dev")
            .description("React frontend")
            .quantity(3)
            .salaryType(SalaryType.HOURLY)
            .budgetMin(BigDecimal.valueOf(30.00))
            .budgetMax(BigDecimal.valueOf(50.00))
            .status(JobStatus.RECRUITING)
            .skills(Set.of(skill))
            .build();
    job.setId(id);

    assertEquals(id, job.getId());
    assertEquals(project, job.getProject());
    assertEquals("Frontend Dev", job.getTitle());
    assertEquals("React frontend", job.getDescription());
    assertEquals(3, job.getQuantity());
    assertEquals(SalaryType.HOURLY, job.getSalaryType());
    assertEquals(BigDecimal.valueOf(30.00), job.getBudgetMin());
    assertEquals(BigDecimal.valueOf(50.00), job.getBudgetMax());
    assertEquals(JobStatus.RECRUITING, job.getStatus());
    assertEquals(1, job.getSkills().size());
    assertTrue(job.getSkills().contains(skill));
  }
}
