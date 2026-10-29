package com.occupify.core.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record ProjectCreatedEvent(
    UUID projectId,
    String title,
    String description,
    String status,
    UUID employerId,
    String employerEmail,
    Instant createdAt)
    implements Serializable {}
