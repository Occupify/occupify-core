package com.occupify.core.dto.request.contract;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateContractRequest(
    @NotNull(message = "Job ID is required") UUID jobId,
    UUID employeeId,
    String freelancerEmail,
    String sourceType,
    UUID sourceId,
    @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,
    @NotNull(message = "Agreed rate is required")
        @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Agreed rate must be greater than 0")
        BigDecimal agreedRate,
    @NotBlank(message = "Salary type is required")
        @Size(max = 50, message = "Salary type cannot exceed 50 characters")
        String salaryType,
    @Size(max = 100, message = "Salary due date cannot exceed 100 characters") String salaryDueDate,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String jobDescription,
    String invitationMessage) {}
