package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportUpdatedEvent(
    UUID reportId, String status, UUID moderatedByAdminId, String note, Instant updatedAt)
    implements Serializable {}
