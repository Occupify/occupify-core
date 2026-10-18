package com.occupify.core.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email, String otpCode, Instant occurredAt)
    implements Serializable {

  public UserRegisteredEvent {
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }
}
