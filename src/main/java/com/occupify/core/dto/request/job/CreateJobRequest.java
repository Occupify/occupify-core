package com.occupify.core.dto.request.job;

import com.occupify.core.enums.SalaryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateJobRequest(
    UUID projectId,
    @NotBlank(message = "Role is required") @Size(max = 100, message = "Role cannot exceed 100 characters") String role,
    @NotBlank(message = "Title is required") @Size(max = 255, message = "Title cannot exceed 255 characters") String title,
    String description,
    BigDecimal budget,
    SalaryType salaryType,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Set<UUID> skillIds) {}
