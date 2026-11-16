package com.occupify.core.dto.request.report;

import com.occupify.core.enums.ReportReasonCategory;
import com.occupify.core.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Request body for submitting a new report")
public record CreateReportRequest(
    @NotNull(message = "Target type is required")
        @Schema(
            description = "Type of target being reported",
            example = "USER",
            requiredMode = Schema.RequiredMode.REQUIRED)
        ReportTargetType targetType,
    @Schema(
            description = "ID of the target user if targetType is USER",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID targetUserId,
    @Schema(
            description = "ID of the target project if targetType is PROJECT",
            example = "550e8400-e29b-41d4-a716-446655440001")
        UUID targetProjectId,
    @NotNull(message = "Reason category is required")
        @Schema(
            description = "Categorical reason for the report",
            example = "SPAM",
            requiredMode = Schema.RequiredMode.REQUIRED)
        ReportReasonCategory reasonCategory,
    @NotBlank(message = "Description cannot be blank")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        @Schema(
            description = "Detailed description explaining why the target is being reported",
            example = "Suspicious activity detected.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String description) {}
