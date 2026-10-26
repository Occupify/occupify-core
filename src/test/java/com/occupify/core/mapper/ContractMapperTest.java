package com.occupify.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.entity.contract.CancellationContract;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ContractMapperTest {

  private final ContractMapper mapper = Mappers.getMapper(ContractMapper.class);

  @Test
  @DisplayName("Should map Contract to ContractSummaryResponse and ContractDetailResponse")
  void shouldMapContractToDto() {
    User employer = User.builder().fullName("Employer Name").email("emp@test.local").build();
    User employee = User.builder().fullName("Freelancer Name").email("free@test.local").build();
    Project project = Project.builder().title("Redesign Mobile App").build();
    Job job = Job.builder().title("Backend Dev").project(project).build();

    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .title("Hợp đồng Backend Dev")
            .agreedRate(new BigDecimal("38000000.00"))
            .salaryType("MONTHLY")
            .salaryDueDate("Ngày 05 hàng tháng")
            .status(ContractStatus.PENDING)
            .termsFileKey("contracts/sample.pdf")
            .startedAt(LocalDateTime.of(2026, 11, 1, 0, 0))
            .finishedAt(LocalDateTime.of(2027, 4, 30, 0, 0))
            .build();

    ContractSummaryResponse summary = mapper.toSummaryResponse(contract);
    assertThat(summary.getTitle()).isEqualTo("Hợp đồng Backend Dev");
    assertThat(summary.getEmployerName()).isEqualTo("Employer Name");
    assertThat(summary.getEmployeeName()).isEqualTo("Freelancer Name");
    assertThat(summary.getProjectTitle()).isEqualTo("Redesign Mobile App");

    String signedUrl = "https://objectstorage.signed.url";
    ContractDetailResponse detail = mapper.toDetailResponse(contract, signedUrl, null);
    assertThat(detail.getTitle()).isEqualTo("Hợp đồng Backend Dev");
    assertThat(detail.getTermsFileUrl()).isEqualTo(signedUrl);
    assertThat(detail.getJob().title()).isEqualTo("Backend Dev");
    assertThat(detail.getProject().title()).isEqualTo("Redesign Mobile App");
  }

  @Test
  @DisplayName("Should map CancellationContract to CancellationContractResponse")
  void shouldMapCancellationContract() {
    User user = User.builder().fullName("Requester Name").build();
    Contract contract = Contract.builder().title("Target Contract").build();
    CancellationContract cancellation =
        CancellationContract.builder()
            .contract(contract)
            .requestedByUser(user)
            .reason("Reason for contract cancellation")
            .evidenceFileKey("evidence/file.pdf")
            .status("ACCEPTED")
            .build();

    String evidenceUrl = "https://evidence.signed.url";
    var response = mapper.toCancellationResponse(cancellation, evidenceUrl);

    assertThat(response.getReason()).isEqualTo("Reason for contract cancellation");
    assertThat(response.getEvidenceFileKey()).isEqualTo("evidence/file.pdf");
    assertThat(response.getEvidenceFileUrl()).isEqualTo(evidenceUrl);
    assertThat(response.getRequestedByUserName()).isEqualTo("Requester Name");
    assertThat(response.getStatus()).isEqualTo("ACCEPTED");
  }
}
