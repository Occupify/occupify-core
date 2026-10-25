package com.occupify.core.dto.request.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CancelContractRequest(
    @NotBlank(message = "Lý do hủy hợp đồng không được để trống")
        @Size(min = 10, message = "Lý do hủy hợp đồng phải có ít nhất 10 ký tự")
        String reason) {}
