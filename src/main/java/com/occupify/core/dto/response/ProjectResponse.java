package com.occupify.core.dto.response;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String title,
    String description,
    BigDecimal startPrice,
    ProjectPeriod period,
    LocalDate dueDate,
    ExperienceLevel experienceLevel,
    ProjectStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime closedAt,
    EmployerSummaryResponse employer,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    Set<String> skills,
    Integer openJobsCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
