package com.occupify.core.service.impl;

import com.occupify.core.dto.request.RejectContractRequest;
import com.occupify.core.dto.response.ContractResponse;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractRejectedEvent;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.ContractMapper;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ContractService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContractServiceImpl implements ContractService {

  private final ContractRepository contractRepository;
  private final ContractMapper contractMapper;
  private final ContractEventProducer contractEventProducer;

  @Override
  @Transactional
  public ContractResponse acceptContract(UUID contractId, UserContext userContext) {
    Contract contract = getContractEntityWithDetails(contractId);
    validateFreelancer(contract, userContext);

    if (contract.getStatus() != ContractStatus.PENDING) {
      log.warn("Contract [{}] has status [{}], expected PENDING", contractId, contract.getStatus());
      throw new ContractException(ContractErrorCode.INVALID_CONTRACT_STATUS);
    }

    contract.setStatus(ContractStatus.PENDING_ESCROW);
    Contract saved = contractRepository.save(contract);
    log.info(
        "Contract [{}] marked as PENDING_ESCROW by freelancer [{}]", contractId, userContext.userId());

    UUID employerId =
        (contract.getJob() != null
                && contract.getJob().getProject() != null
                && contract.getJob().getProject().getEmployer() != null)
            ? contract.getJob().getProject().getEmployer().getId()
            : null;
    UUID jobId = contract.getJob() != null ? contract.getJob().getId() : null;
    UUID projectId =
        (contract.getJob() != null && contract.getJob().getProject() != null)
            ? contract.getJob().getProject().getId()
            : null;

    ContractAcceptedEvent event =
        new ContractAcceptedEvent(
            UUID.randomUUID(),
            saved.getId(),
            jobId,
            projectId,
            employerId,
            userContext.userId(),
            saved.getTitle(),
            saved.getBudget(),
            saved.getSalaryType(),
            saved.getStartDate(),
            saved.getEndDate(),
            LocalDateTime.now());

    contractEventProducer.publishContractAccepted(event);

    return contractMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ContractResponse rejectContract(
      UUID contractId, RejectContractRequest request, UserContext userContext) {
    Contract contract = getContractEntityWithDetails(contractId);
    validateFreelancer(contract, userContext);

    if (contract.getStatus() != ContractStatus.PENDING) {
      log.warn("Contract [{}] has status [{}], expected PENDING", contractId, contract.getStatus());
      throw new ContractException(ContractErrorCode.INVALID_CONTRACT_STATUS);
    }

    contract.setStatus(ContractStatus.CANCELLED);
    Contract saved = contractRepository.save(contract);
    log.info(
        "Contract [{}] marked as CANCELLED (rejected) by freelancer [{}]",
        contractId,
        userContext.userId());

    UUID employerId =
        (contract.getJob() != null
                && contract.getJob().getProject() != null
                && contract.getJob().getProject().getEmployer() != null)
            ? contract.getJob().getProject().getEmployer().getId()
            : null;
    UUID jobId = contract.getJob() != null ? contract.getJob().getId() : null;

    String reason =
        (request != null && request.reason() != null && !request.reason().isBlank())
            ? request.reason()
            : "Rejected by freelancer";

    ContractRejectedEvent event =
        new ContractRejectedEvent(
            UUID.randomUUID(),
            saved.getId(),
            jobId,
            employerId,
            userContext.userId(),
            reason,
            LocalDateTime.now());

    contractEventProducer.publishContractRejected(event);

    return contractMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ContractResponse getContract(UUID contractId, UserContext userContext) {
    Contract contract = getContractEntityWithDetails(contractId);
    return contractMapper.toResponse(contract);
  }

  @Override
  @Transactional
  public void confirmContractEscrow(UUID contractId) {
    Contract contract = contractRepository.findById(contractId).orElse(null);
    if (contract == null) {
      log.warn("[Saga Escrow] Contract [{}] not found for activation", contractId);
      return;
    }

    if (contract.getStatus() == ContractStatus.PENDING_ESCROW) {
      log.info(
          "[Saga Escrow] Escrow confirmed for contract [{}]. Transitioning to IN_PROGRESS",
          contractId);
      contract.setStatus(ContractStatus.IN_PROGRESS);
      contract.setStartDate(LocalDate.now());
      contractRepository.save(contract);
    } else {
      log.warn(
          "[Saga Escrow] Contract [{}] has status [{}], expected PENDING_ESCROW for activation",
          contractId,
          contract.getStatus());
    }
  }

  @Override
  @Transactional
  public void compensateContract(UUID contractId, String reason) {
    Contract contract = contractRepository.findById(contractId).orElse(null);
    if (contract == null) {
      log.warn("[Saga Compensation] Contract [{}] not found for rollback", contractId);
      return;
    }

    if (contract.getStatus() == ContractStatus.IN_PROGRESS
        || contract.getStatus() == ContractStatus.PENDING_ESCROW) {
      log.warn(
          "[Saga Compensation] Rolling back contract [{}] from [{}] to PENDING. Reason: {}",
          contractId,
          contract.getStatus(),
          reason);
      contract.setStatus(ContractStatus.PENDING);
      contract.setStartDate(null);
      contractRepository.save(contract);
    }
  }

  private Contract getContractEntityWithDetails(UUID contractId) {
    return contractRepository
        .findByIdWithDetails(contractId)
        .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));
  }

  private void validateFreelancer(Contract contract, UserContext userContext) {
    User freelancer = contract.getFreelancer();
    if (freelancer == null || !freelancer.getId().equals(userContext.userId())) {
      log.warn(
          "User [{}] is not assigned freelancer for contract [{}]",
          userContext.userId(),
          contract.getId());
      throw new ContractException(ContractErrorCode.FORBIDDEN_OPERATION);
    }
  }
}
