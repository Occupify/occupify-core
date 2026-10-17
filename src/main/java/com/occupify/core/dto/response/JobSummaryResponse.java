package com.occupify.core.dto.response;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record JobSummaryResponse(
    UUID id,
    String title,
    String description,
    Integer quantity,
    SalaryType salaryType,
    BigDecimal budgetMin,
    BigDecimal budgetMax,
    JobStatus status,
    Set<String> skills,
    LocalDateTime createdAt) {}
