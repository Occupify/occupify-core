package com.occupify.core.dto.request;

import com.occupify.core.enums.BidStatus;
import java.util.UUID;

public record BidFilterRequest(
    UUID jobId,
    UUID projectId,
    BidStatus status,
    Boolean mine
) {}
