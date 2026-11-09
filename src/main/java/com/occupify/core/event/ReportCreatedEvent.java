package com.occupify.core.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportCreatedEvent(
    UUID reportId,
    UUID reporterUserId,
    String targetType,
    UUID targetUserId,
    UUID targetProjectId,
    String reasonCategory,
    String description,
    List<String> evidenceUrls,
    String status,
    Instant createdAt)
    implements Serializable {}
