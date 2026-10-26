package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserActivatedEvent(
    UUID userId, String email, String fullName, String role, String status, Instant occurredAt)
    implements Serializable {

  public UserActivatedEvent {
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }
}
