package com.occupify.core.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedJob(
    String id,
    String title,
    String description,
    Integer quantity,
    SalaryType salaryType,
    BigDecimal budgetMin,
    BigDecimal budgetMax,
    JobStatus status,
    List<String> skills,
    List<String> requirements) {
  public SeedJob {
    if (quantity == null) {
      quantity = 1;
    }
    if (status == null) {
      status = JobStatus.RECRUITING;
    }
    if (skills == null) {
      skills = List.of();
    }
    if (requirements == null) {
      requirements = List.of();
    }
  }
}
