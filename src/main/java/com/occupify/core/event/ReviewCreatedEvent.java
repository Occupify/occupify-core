package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewCreatedEvent(
    UUID eventId,
    UUID reviewId,
    UUID contractId,
    UUID reviewerId,
    String reviewerName,
    UUID revieweeId,
    String revieweeName,
    String revieweeEmail,
    Integer rating,
    String comment,
    LocalDateTime timestamp)
    implements Serializable {}
