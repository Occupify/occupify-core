package com.occupify.core.entity.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContractTest {

  @Test
  void testContractBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    Job job = Job.builder().title("Dev").build();
    User employer = User.builder().email("employer@test.com").build();
    User employee = User.builder().email("employee@test.com").build();
    LocalDateTime startedAt = LocalDateTime.of(2026, 1, 1, 9, 0);
    LocalDateTime finishedAt = LocalDateTime.of(2026, 6, 1, 18, 0);

    Contract contract =
        Contract.builder()
            .job(job)
            .employer(employer)
            .employee(employee)
            .sourceType("JOB_BID")
            .sourceId(sourceId)
            .agreedRate(BigDecimal.valueOf(1500.00))
            .salaryType("MONTHLY")
            .status(ContractStatus.IN_PROGRESS)
            .paymentStatus("ESCROWED")
            .startedAt(startedAt)
            .finishedAt(finishedAt)
            .build();
    contract.setId(id);

    assertEquals(id, contract.getId());
    assertEquals(job, contract.getJob());
    assertEquals(employer, contract.getEmployer());
    assertEquals(employee, contract.getEmployee());
    assertEquals("JOB_BID", contract.getSourceType());
    assertEquals(sourceId, contract.getSourceId());
    assertEquals(BigDecimal.valueOf(1500.00), contract.getAgreedRate());
    assertEquals("MONTHLY", contract.getSalaryType());
    assertEquals(ContractStatus.IN_PROGRESS, contract.getStatus());
    assertEquals("ESCROWED", contract.getPaymentStatus());
    assertEquals(startedAt, contract.getStartedAt());
    assertEquals(finishedAt, contract.getFinishedAt());
  }
}
