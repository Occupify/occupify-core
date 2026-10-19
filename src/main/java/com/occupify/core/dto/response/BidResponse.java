package com.occupify.core.dto.response;

import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidResponse(
    UUID id,
    BidJobSummaryResponse job,
    BidProjectSummaryResponse project,
    FreelancerSummaryResponse freelancer,
    SalaryType proposedSalaryType,
    BigDecimal proposedRate,
    String commitmentDuration,
    String weeklyCommitmentHours,
    String message,
    String attachmentCvUrl,
    String portfolioLink,
    BidStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
