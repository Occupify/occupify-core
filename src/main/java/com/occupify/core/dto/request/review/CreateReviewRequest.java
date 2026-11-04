package com.occupify.core.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateReviewRequest(
    @NotNull(message = "Contract ID is required") UUID contractId,
    @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating,
    @Size(max = 1000, message = "Comment must not exceed 1000 characters") String comment) {}
