package com.occupify.core.dto.response.report;

import com.occupify.core.enums.ReportReasonCategory;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Response details of a submitted report")
public record ReportResponse(
    @Schema(
            description = "Unique ID of the report",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
    @Schema(
            description = "User ID of the reporter",
            example = "550e8400-e29b-41d4-a716-446655440001")
        UUID reporterId,
    @Schema(description = "Type of entity reported", example = "USER") ReportTargetType targetType,
    @Schema(
            description = "Target user ID if reported entity is a user",
            example = "550e8400-e29b-41d4-a716-446655440002")
        UUID targetUserId,
    @Schema(
            description = "Target project ID if reported entity is a project",
            example = "550e8400-e29b-41d4-a716-446655440003")
        UUID targetProjectId,
    @Schema(description = "Categorical reason for the report", example = "SPAM")
        ReportReasonCategory reasonCategory,
    @Schema(description = "Detailed report explanation", example = "Suspicious activity detected.")
        String description,
    @Schema(description = "Current moderation status of the report", example = "PENDING")
        ReportStatus status,
    @Schema(description = "List of uploaded evidence file URLs") List<String> evidenceUrls,
    @Schema(description = "Timestamp when the report was submitted") LocalDateTime createdAt) {}
