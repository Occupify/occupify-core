package com.occupify.core.dto.response.job;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record JobDetailResponse(
    UUID id,
    UUID projectId,
    String projectTitle,
    String role,
    String title,
    String description,
    BigDecimal budget,
    SalaryType salaryType,
    String termsFileUrl,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    JobStatus status,
    Integer quantity,
    Integer activeBidsCount,
    Set<String> skills,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
