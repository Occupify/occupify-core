package com.occupify.core.dto.response.contract;

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
public class ContractSummaryResponse {
  private UUID id;
  private String title;
  private UUID employerId;
  private String employerName;
  private UUID employeeId;
  private String employeeName;
  private UUID projectId;
  private String projectTitle;
  private UUID jobId;
  private String jobTitle;
  private BigDecimal agreedRate;
  private String salaryType;
  private String salaryDueDate;
  private ContractStatus status;
  private String paymentStatus;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
