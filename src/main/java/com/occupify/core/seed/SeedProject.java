package com.occupify.core.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedProject(
    String id,
    String title,
    String description,
    BigDecimal startPrice,
    ProjectPeriod period,
    LocalDate dueDate,
    ExperienceLevel experienceLevel,
    ProjectStatus status,
    String location,
    BigDecimal hoursPerDay,
    String duration,
    List<String> tags,
    List<SeedJob> jobs
) {
  public SeedProject {
    if (status == null) {
      status = ProjectStatus.OPEN;
    }
    if (tags == null) {
      tags = List.of();
    }
    if (jobs == null) {
      jobs = List.of();
    }
  }
}
