package com.occupify.core.dto.response;

import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractResponse(
    UUID id,
    UUID jobId,
    UUID projectId,
    UUID employerId,
    UUID freelancerId,
    String title,
    String description,
    BigDecimal budget,
    SalaryType salaryType,
    LocalDate startDate,
    LocalDate endDate,
    String attachmentUrl,
    ContractStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
