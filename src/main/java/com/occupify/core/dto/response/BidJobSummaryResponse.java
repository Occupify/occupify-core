package com.occupify.core.dto.response;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.util.UUID;

public record BidJobSummaryResponse(
    UUID id,
    String title,
    SalaryType salaryType,
    BigDecimal budgetMin,
    BigDecimal budgetMax,
    JobStatus status) {}
