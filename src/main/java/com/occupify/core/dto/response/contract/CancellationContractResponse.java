package com.occupify.core.dto.response.contract;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationContractResponse {
  private UUID id;
  private UUID contractId;
  private UUID requestedByUserId;
  private String requestedByUserName;
  private String reason;
  private String evidenceFileKey;
  private String evidenceFileUrl;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
