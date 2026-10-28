package com.occupify.core.event.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobStatusChangedDomainEvent(
    UUID eventId,
    UUID projectId,
    UUID jobId,
    Long jobVersion,
    String oldStatus,
    String newStatus,
    LocalDateTime occurredAt) {}
