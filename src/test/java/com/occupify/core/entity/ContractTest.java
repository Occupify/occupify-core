package com.occupify.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContractTest {

  @Test
  void testContractBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    Job job = Job.builder().title("Dev").build();
    User freelancer = User.builder().email("free@test.com").build();
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 6, 1);

    Contract contract =
        Contract.builder()
            .job(job)
            .freelancer(freelancer)
            .title("Full Stack Contract")
            .description("Scope of work")
            .budget(BigDecimal.valueOf(1500.00))
            .salaryType(SalaryType.MONTHLY)
            .startDate(start)
            .endDate(end)
            .attachmentUrl("https://example.com/contract.pdf")
            .status(ContractStatus.IN_PROGRESS)
            .build();
    contract.setId(id);

    assertEquals(id, contract.getId());
    assertEquals(job, contract.getJob());
    assertEquals(freelancer, contract.getFreelancer());
    assertEquals("Full Stack Contract", contract.getTitle());
    assertEquals("Scope of work", contract.getDescription());
    assertEquals(BigDecimal.valueOf(1500.00), contract.getBudget());
    assertEquals(SalaryType.MONTHLY, contract.getSalaryType());
    assertEquals(start, contract.getStartDate());
    assertEquals(end, contract.getEndDate());
    assertEquals("https://example.com/contract.pdf", contract.getAttachmentUrl());
    assertEquals(ContractStatus.IN_PROGRESS, contract.getStatus());
  }
}
