package com.occupify.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.dto.request.RejectContractRequest;
import com.occupify.core.dto.response.ContractResponse;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.SalaryType;
import com.occupify.core.enums.UserRole;
import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractRejectedEvent;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.ContractMapper;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.impl.ContractServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

  @Mock private ContractRepository contractRepository;
  @Mock private ContractMapper contractMapper;
  @Mock private ContractEventProducer contractEventProducer;

  @InjectMocks private ContractServiceImpl contractService;

  private UUID contractId;
  private UUID freelancerId;
  private UUID employerId;
  private User freelancer;
  private User employer;
  private Project project;
  private Job job;
  private Contract contract;
  private UserContext freelancerContext;

  @BeforeEach
  void setUp() {
    contractId = UUID.randomUUID();
    freelancerId = UUID.randomUUID();
    employerId = UUID.randomUUID();

    freelancer = User.builder().email("freelancer@test.com").role(UserRole.USER).build();
    freelancer.setId(freelancerId);

    employer = User.builder().email("employer@test.com").role(UserRole.USER).build();
    employer.setId(employerId);

    project = Project.builder().title("Awesome Project").employer(employer).build();
    project.setId(UUID.randomUUID());

    job = Job.builder().title("Backend Dev").project(project).build();
    job.setId(UUID.randomUUID());

    contract =
        Contract.builder()
            .job(job)
            .freelancer(freelancer)
            .title("Backend Contract")
            .budget(BigDecimal.valueOf(2000.00))
            .salaryType(SalaryType.MONTHLY)
            .status(ContractStatus.PENDING)
            .build();
    contract.setId(contractId);

    freelancerContext = new UserContext(freelancerId, UserRole.USER, "freelancer@test.com");
  }

  @Test
  @DisplayName("Accept contract - Success")
  void testAcceptContract_Success() {
    when(contractRepository.findByIdWithDetails(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ContractResponse mockResponse =
        new ContractResponse(
            contractId,
            job.getId(),
            project.getId(),
            employerId,
            freelancerId,
            "Backend Contract",
            null,
            BigDecimal.valueOf(2000.00),
            SalaryType.MONTHLY,
            null,
            null,
            null,
            ContractStatus.PENDING_ESCROW,
            LocalDateTime.now(),
            LocalDateTime.now());
    when(contractMapper.toResponse(any(Contract.class))).thenReturn(mockResponse);

    ContractResponse response = contractService.acceptContract(contractId, freelancerContext);

    assertNotNull(response);
    assertEquals(ContractStatus.PENDING_ESCROW, response.status());
    assertEquals(ContractStatus.PENDING_ESCROW, contract.getStatus());
    assertNull(contract.getStartDate());

    ArgumentCaptor<ContractAcceptedEvent> eventCaptor =
        ArgumentCaptor.forClass(ContractAcceptedEvent.class);
    verify(contractEventProducer).publishContractAccepted(eventCaptor.capture());

    ContractAcceptedEvent published = eventCaptor.getValue();
    assertEquals(contractId, published.contractId());
    assertEquals(employerId, published.employerId());
    assertEquals(freelancerId, published.freelancerId());
    assertEquals(BigDecimal.valueOf(2000.00), published.budget());
    assertEquals(SalaryType.MONTHLY, published.salaryType());
  }

  @Test
  @DisplayName("Accept contract - Fails when caller is not assigned freelancer")
  void testAcceptContract_Forbidden_WhenNotFreelancer() {
    UserContext otherUserContext =
        new UserContext(UUID.randomUUID(), UserRole.USER, "other@test.com");
    when(contractRepository.findByIdWithDetails(contractId)).thenReturn(Optional.of(contract));

    ContractException ex =
        assertThrows(
            ContractException.class,
            () -> contractService.acceptContract(contractId, otherUserContext));

    assertEquals(ContractErrorCode.FORBIDDEN_OPERATION.code(), ex.getCode());
    verify(contractRepository, never()).save(any());
    verify(contractEventProducer, never()).publishContractAccepted(any());
  }

  @Test
  @DisplayName("Accept contract - Fails when contract is not in PENDING status")
  void testAcceptContract_InvalidStatus_WhenNotInPending() {
    contract.setStatus(ContractStatus.IN_PROGRESS);
    when(contractRepository.findByIdWithDetails(contractId)).thenReturn(Optional.of(contract));

    ContractException ex =
        assertThrows(
            ContractException.class,
            () -> contractService.acceptContract(contractId, freelancerContext));

    assertEquals(ContractErrorCode.INVALID_CONTRACT_STATUS.code(), ex.getCode());
    verify(contractRepository, never()).save(any());
    verify(contractEventProducer, never()).publishContractAccepted(any());
  }

  @Test
  @DisplayName("Reject contract - Success")
  void testRejectContract_Success() {
    when(contractRepository.findByIdWithDetails(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ContractResponse mockResponse =
        new ContractResponse(
            contractId,
            job.getId(),
            project.getId(),
            employerId,
            freelancerId,
            "Backend Contract",
            null,
            BigDecimal.valueOf(2000.00),
            SalaryType.MONTHLY,
            null,
            null,
            null,
            ContractStatus.CANCELLED,
            LocalDateTime.now(),
            LocalDateTime.now());
    when(contractMapper.toResponse(any(Contract.class))).thenReturn(mockResponse);

    RejectContractRequest request = new RejectContractRequest("Salary is below expectations");
    ContractResponse response =
        contractService.rejectContract(contractId, request, freelancerContext);

    assertNotNull(response);
    assertEquals(ContractStatus.CANCELLED, response.status());
    assertEquals(ContractStatus.CANCELLED, contract.getStatus());

    ArgumentCaptor<ContractRejectedEvent> eventCaptor =
        ArgumentCaptor.forClass(ContractRejectedEvent.class);
    verify(contractEventProducer).publishContractRejected(eventCaptor.capture());

    ContractRejectedEvent published = eventCaptor.getValue();
    assertEquals(contractId, published.contractId());
    assertEquals(employerId, published.employerId());
    assertEquals(freelancerId, published.freelancerId());
    assertEquals("Salary is below expectations", published.reason());
  }

  @Test
  @DisplayName("Saga Compensation - Reverts IN_PROGRESS back to PENDING")
  void testCompensateContract_Success() {
    contract.setStatus(ContractStatus.IN_PROGRESS);
    contract.setStartDate(LocalDate.now());

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    contractService.compensateContract(contractId, "Payment service failed to lock escrow");

    assertEquals(ContractStatus.PENDING, contract.getStatus());
    assertNull(contract.getStartDate());
    verify(contractRepository).save(contract);
  }

  @Test
  @DisplayName("Saga Compensation - Reverts PENDING_ESCROW back to PENDING")
  void testCompensateContract_FromPendingEscrow() {
    contract.setStatus(ContractStatus.PENDING_ESCROW);

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    contractService.compensateContract(contractId, "Payment service timeout");

    assertEquals(ContractStatus.PENDING, contract.getStatus());
    assertNull(contract.getStartDate());
    verify(contractRepository).save(contract);
  }

  @Test
  @DisplayName("Saga Confirmation - Confirms escrow and transitions PENDING_ESCROW to IN_PROGRESS")
  void testConfirmContractEscrow_Success() {
    contract.setStatus(ContractStatus.PENDING_ESCROW);

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    contractService.confirmContractEscrow(contractId);

    assertEquals(ContractStatus.IN_PROGRESS, contract.getStatus());
    assertNotNull(contract.getStartDate());
    verify(contractRepository).save(contract);
  }

  @Test
  @DisplayName("Saga Compensation - Ignores contracts not in IN_PROGRESS status")
  void testCompensateContract_IgnoredWhenNotInProgress() {
    contract.setStatus(ContractStatus.CANCELLED);
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    contractService.compensateContract(contractId, "Some error");

    assertEquals(ContractStatus.CANCELLED, contract.getStatus());
    verify(contractRepository, never()).save(any());
  }
}
