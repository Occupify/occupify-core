package com.occupify.core.event;

import com.occupify.core.enums.SalaryType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractAcceptedEvent(
    UUID eventId,
    UUID contractId,
    UUID jobId,
    UUID projectId,
    UUID employerId,
    UUID freelancerId,
    String title,
    BigDecimal budget,
    SalaryType salaryType,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime timestamp)
    implements Serializable {}
