package com.occupify.core.dto.request.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Request body for candidate rejecting a contract offer")
public record RejectContractRequest(
    @Schema(
            description = "Reason for rejecting the contract",
            example = "Mức thù lao chưa phù hợp với phạm vi công việc đề xuất")
        @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự")
        String reason) {}
