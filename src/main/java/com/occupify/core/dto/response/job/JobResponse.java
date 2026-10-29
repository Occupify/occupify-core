package com.occupify.core.dto.response.job;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record JobResponse(
    UUID id,
    UUID projectId,
    String role,
    String title,
    String description,
    BigDecimal budget,
    SalaryType salaryType,
    String termsFileUrl,
    JobStatus status,
    Integer quantity,
    Integer activeBidsCount,
    Set<String> skills,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    LocalDateTime createdAt) {}
