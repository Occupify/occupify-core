package com.occupify.core.dto.response;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record JobSummaryResponse(
    UUID id,
    UUID projectId,
    String title,
    String description,
    Integer quantity,
    SalaryType salaryType,
    BigDecimal budgetMin,
    BigDecimal budgetMax,
    JobStatus status,
    Integer bidCount,
    Set<String> skills,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
