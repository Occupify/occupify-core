package com.occupify.core.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record ProjectClosedEvent(
    UUID projectId,
    String title,
    UUID employerId,
    String employerEmail,
    UUID actorId,
    String actorRole,
    Instant closedAt)
    implements Serializable {}
