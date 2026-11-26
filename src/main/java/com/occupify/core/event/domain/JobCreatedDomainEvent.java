package com.occupify.core.event.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobCreatedDomainEvent(
    UUID eventId,
    UUID projectId,
    UUID jobId,
    String status,
    LocalDateTime occurredAt) {}
