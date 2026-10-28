package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractCreatedEvent(
    UUID eventId,
    UUID contractId,
    UUID jobId,
    UUID projectId,
    UUID employerId,
    UUID employeeId,
    String freelancerEmail,
    String invitationMessage,
    String role,
    String title,
    BigDecimal agreedRate,
    String salaryType,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    LocalDateTime createdAt,
    String employerName,
    String employerEmail)
    implements Serializable {

  public ContractCreatedEvent(
      UUID eventId,
      UUID contractId,
      UUID jobId,
      UUID projectId,
      UUID employerId,
      UUID employeeId,
      String freelancerEmail,
      String invitationMessage,
      String role,
      String title,
      BigDecimal agreedRate,
      String salaryType,
      LocalDateTime startedAt,
      LocalDateTime finishedAt,
      LocalDateTime createdAt) {
    this(
        eventId,
        contractId,
        jobId,
        projectId,
        employerId,
        employeeId,
        freelancerEmail,
        invitationMessage,
        role,
        title,
        agreedRate,
        salaryType,
        startedAt,
        finishedAt,
        createdAt,
        null,
        null);
  }
}
