package com.occupify.core.dto.response;

import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.time.LocalDate;
import java.util.UUID;

public record BidProjectSummaryResponse(
    UUID id,
    String title,
    ProjectPeriod period,
    ProjectStatus status,
    LocalDate dueDate,
    EmployerSummaryResponse employer) {}
