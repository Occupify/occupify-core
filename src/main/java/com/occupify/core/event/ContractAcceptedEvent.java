package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.occupify.core.enums.SalaryType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    LocalDateTime timestamp,
    String employerEmail,
    String employerName,
    String freelancerName,
    String freelancerEmail)
    implements Serializable {

  public ContractAcceptedEvent(
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
      LocalDateTime timestamp) {
    this(
        eventId,
        contractId,
        jobId,
        projectId,
        employerId,
        freelancerId,
        title,
        budget,
        salaryType,
        startDate,
        endDate,
        timestamp,
        null,
        null,
        null,
        null);
  }
}
