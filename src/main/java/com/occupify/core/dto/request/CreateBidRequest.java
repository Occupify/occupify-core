package com.occupify.core.dto.request;

import com.occupify.core.enums.SalaryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public record CreateBidRequest(
    @NotNull(message = "Job ID is required") UUID jobId,
    SalaryType proposedSalaryType,
    @NotNull(message = "Proposed rate is required")
        @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Proposed rate must be greater than 0")
        BigDecimal proposedRate,
    @Size(max = 100, message = "Commitment duration cannot exceed 100 characters")
        String commitmentDuration,
    @Size(max = 50, message = "Weekly commitment hours cannot exceed 50 characters")
        String weeklyCommitmentHours,
    @NotBlank(message = "Proposal message cannot be blank")
        @Size(max = 5000, message = "Proposal message cannot exceed 5000 characters")
        String message,
    @Nullable MultipartFile attachmentCvUrl,
    @Size(max = 255, message = "Portfolio link cannot exceed 255 characters")
        String portfolioLink) {}
