package com.occupify.core.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidCreatedEvent(
    UUID bidId,
    UUID jobId,
    String jobTitle,
    UUID projectId,
    String projectTitle,
    UUID employerId,
    UUID freelancerId,
    String freelancerName,
    BigDecimal proposedRate,
    LocalDateTime createdAt) {}
