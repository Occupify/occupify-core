package com.occupify.core.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectCreatedEvent(
    UUID projectId, String title, UUID employerId, String employerEmail, LocalDateTime createdAt)
    implements Serializable {}
