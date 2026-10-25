package com.occupify.core.dto.response.contract;

import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.JobSummaryResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.enums.ContractStatus;
import java.math.BigDecimal;
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
public class ContractDetailResponse {
  private UUID id;
  private String title;
  private EmployerSummaryResponse employer;
  private EmployerSummaryResponse employee;
  private ProjectResponse project;
  private JobSummaryResponse job;
  private String sourceType;
  private UUID sourceId;
  private BigDecimal agreedRate;
  private String salaryType;
  private String salaryDueDate;
  private String jobDescription;
  private ContractStatus status;
  private String paymentStatus;
  private String termsFileKey;
  private String termsFileUrl;
  private CancellationContractResponse cancellation;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
