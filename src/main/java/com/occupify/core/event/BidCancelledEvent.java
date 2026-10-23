package com.occupify.core.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record BidCancelledEvent(
    UUID bidId,
    UUID jobId,
    UUID projectId,
    UUID employerId,
    UUID freelancerId,
    LocalDateTime cancelledAt) {}
