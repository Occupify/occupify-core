package com.occupify.core.dto.response;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    LocalDateTime closedAt,
    EmployerSummaryResponse employer,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
