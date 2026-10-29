package com.occupify.core.event.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectUpdatedDomainEvent(
    UUID eventId, UUID projectId, Long version, LocalDateTime occurredAt) {}
