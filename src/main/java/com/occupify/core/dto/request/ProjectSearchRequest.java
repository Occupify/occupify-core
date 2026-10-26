package com.occupify.core.dto.request;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProjectSearchRequest(
    String keyword,
    ProjectStatus status,
    UUID employerId,
    ProjectPeriod period,
    ExperienceLevel experienceLevel,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<String> skills,
    Integer page,
    Integer size,
    String sort) {

  public ProjectFilterRequest toFilter() {
    return new ProjectFilterRequest(
        keyword, status, employerId, period, experienceLevel, minPrice, maxPrice, skills);
  }
}
