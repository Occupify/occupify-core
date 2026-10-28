package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractRejectedEvent(
    UUID eventId,
    UUID contractId,
    UUID jobId,
    UUID employerId,
    UUID freelancerId,
    String reason,
    LocalDateTime timestamp,
    String employerEmail,
    String employerName,
    String freelancerName,
    String jobTitle)
    implements Serializable {

  public ContractRejectedEvent(
      UUID eventId,
      UUID contractId,
      UUID jobId,
      UUID employerId,
      UUID freelancerId,
      String reason,
      LocalDateTime timestamp) {
    this(
        eventId,
        contractId,
        jobId,
        employerId,
        freelancerId,
        reason,
        timestamp,
        null,
        null,
        null,
        null);
  }
}
