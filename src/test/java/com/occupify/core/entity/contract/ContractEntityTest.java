package com.occupify.core.entity.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.repository.contract.CancellationContractRepository;
import com.occupify.core.repository.contract.ContractRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ContractEntityTest {

  @Autowired private TestEntityManager entityManager;
  @Autowired private ContractRepository contractRepository;
  @Autowired private CancellationContractRepository cancellationContractRepository;

  @Test
  @DisplayName("Should persist contract with new fields and save cancellation request")
  void shouldPersistContractAndCancellation() {
    User employer =
        entityManager.persist(
            User.builder()
                .email("employer@test.local")
                .fullName("Employer Test")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    Project project =
        entityManager.persist(
            Project.builder()
                .employer(employer)
                .title("Project Test")
                .status(ProjectStatus.OPEN)
                .build());

    Job job =
        entityManager.persist(
            Job.builder()
                .project(project)
                .title("Backend Dev")
                .quantity(1)
                .status(JobStatus.RECRUITING)
                .build());
    entityManager.flush();
    Contract contract =
        Contract.builder()
            .employer(employer)
            .job(job)
            .title("Hợp đồng Backend Dev")
            .agreedRate(new BigDecimal("25000000.00"))
            .salaryType("MONTHLY")
            .salaryDueDate("Ngày 05 hàng tháng")
            .jobDescription("Mô tả công việc chi tiết")
            .termsFileKey("contracts/sample-key.pdf")
            .status(ContractStatus.PENDING)
            .build();

    Contract savedContract = contractRepository.save(contract);
    assertThat(savedContract.getId()).isNotNull();
    assertThat(savedContract.getTitle()).isEqualTo("Hợp đồng Backend Dev");
    assertThat(savedContract.getTermsFileKey()).isEqualTo("contracts/sample-key.pdf");

    CancellationContract cancelReq =
        CancellationContract.builder()
            .contract(savedContract)
            .requestedByUser(employer)
            .reason("Freelancer không phản hồi thỏa thuận sau 7 ngày.")
            .evidenceFileKey("cancellations/evidence-123.png")
            .status("ACCEPTED")
            .build();

    CancellationContract savedCancel = cancellationContractRepository.save(cancelReq);
    assertThat(savedCancel.getId()).isNotNull();
    assertThat(savedCancel.getReason()).contains("không phản hồi");
    assertThat(savedCancel.getEvidenceFileKey()).isEqualTo("cancellations/evidence-123.png");

    List<CancellationContract> cancels =
        cancellationContractRepository.findByContractIdOrderByCreatedAtDesc(savedContract.getId());
    assertThat(cancels).hasSize(1);

    Optional<CancellationContract> latestCancel =
        cancellationContractRepository.findFirstByContractIdOrderByCreatedAtDesc(
            savedContract.getId());
    assertThat(latestCancel).isPresent();
    assertThat(latestCancel.get().getId()).isEqualTo(savedCancel.getId());
  }

  @Test
  @DisplayName("Should query contracts by employer and employee with pagination")
  void shouldQueryContractsWithPagination() {
    User employer =
        entityManager.persist(
            User.builder()
                .email("employer2@test.local")
                .fullName("Employer Two")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    User employee =
        entityManager.persist(
            User.builder()
                .email("employee2@test.local")
                .fullName("Employee Two")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    Project project =
        entityManager.persist(
            Project.builder()
                .employer(employer)
                .title("Project Two")
                .status(ProjectStatus.OPEN)
                .build());

    Job job =
        entityManager.persist(
            Job.builder()
                .project(project)
                .title("Frontend Dev")
                .quantity(1)
                .status(JobStatus.RECRUITING)
                .build());
    entityManager.flush();

    Contract contract =
        contractRepository.save(
            Contract.builder()
                .employer(employer)
                .employee(employee)
                .job(job)
                .title("Hợp đồng Frontend Dev")
                .agreedRate(new BigDecimal("18000000.00"))
                .salaryType("MONTHLY")
                .salaryDueDate("Ngày 10 hàng tháng")
                .jobDescription("Frontend Vue/React developer")
                .status(ContractStatus.IN_PROGRESS)
                .build());

    Page<Contract> employerContracts =
        contractRepository.findByEmployerId(employer.getId(), PageRequest.of(0, 10));
    assertThat(employerContracts.getContent()).hasSize(1);
    assertThat(employerContracts.getContent().get(0).getId()).isEqualTo(contract.getId());

    Page<Contract> employeeContracts =
        contractRepository.findByEmployeeId(employee.getId(), PageRequest.of(0, 10));
    assertThat(employeeContracts.getContent()).hasSize(1);
    assertThat(employeeContracts.getContent().get(0).getId()).isEqualTo(contract.getId());

    Page<Contract> statusContracts =
        contractRepository.findByEmployerIdAndStatus(
            employer.getId(), ContractStatus.IN_PROGRESS, PageRequest.of(0, 10));
    assertThat(statusContracts.getContent()).hasSize(1);
  }
}
