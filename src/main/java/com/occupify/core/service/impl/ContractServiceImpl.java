package com.occupify.core.service.impl;

import com.occupify.core.dto.request.contract.CompleteContractRequest;
import com.occupify.core.dto.request.contract.ContractFilterRequest;
import com.occupify.core.dto.request.contract.CreateContractRequest;
import com.occupify.core.dto.request.contract.UpdateContractRequest;
import com.occupify.core.dto.response.contract.CancellationContractResponse;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.entity.bid.Bid;
import com.occupify.core.entity.contract.CancellationContract;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.offer.Offer;
import com.occupify.core.entity.review.Review;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.OfferStatus;
import com.occupify.core.enums.SalaryType;
import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractRejectedEvent;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.ContractMapper;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.bid.BidRepository;
import com.occupify.core.repository.contract.CancellationContractRepository;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.contract.ContractSpecification;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.offer.OfferRepository;
import com.occupify.core.repository.review.ReviewRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ContractService;
import com.occupify.core.service.JobLifecycleService;
import com.occupify.core.service.StorageService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractServiceImpl implements ContractService {

  private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);
  private static final int MIN_REASON_LENGTH = 10;

  private final ContractRepository contractRepository;
  private final CancellationContractRepository cancellationContractRepository;
  private final JobRepository jobRepository;
  private final UserRepository userRepository;
  private final BidRepository bidRepository;
  private final OfferRepository offerRepository;
  private final ReviewRepository reviewRepository;
  private final StorageService storageService;
  private final ContractMapper contractMapper;
  private final ContractEventProducer contractEventProducer;
  private final JobLifecycleService jobLifecycleService;

  @Override
  public ContractDetailResponse createContract(
      CreateContractRequest request, MultipartFile file, UserContext userContext) {
    log.info(
        "[ContractService] Creating contract: jobId={}, user={}",
        request.jobId(),
        userContext.userId());

    User employer =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));

    Job job =
        jobRepository
            .findWithProjectById(request.jobId())
            .or(() -> jobRepository.findById(request.jobId()))
            .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_OWNED));

    if (!userContext.isAdmin()
        && (job.getProject() == null
            || job.getProject().getEmployer() == null
            || !job.getProject().getEmployer().getId().equals(employer.getId()))) {
      throw new ContractException(ContractErrorCode.JOB_NOT_OWNED);
    }

    User employee = resolveEmployee(request);
    String freelancerEmail =
        request.freelancerGmail() != null && !request.freelancerGmail().isBlank()
            ? request.freelancerGmail().trim()
            : employee.getEmail();

    // Auto-close the Job from project
    job.setStatus(JobStatus.CLOSED);
    jobRepository.save(job);

    // Reject other pending proposals for this job and accept candidate's bid if present
    List<Bid> bids = bidRepository.findByJobId(job.getId());
    if (bids != null) {
      for (Bid bid : bids) {
        boolean isCandidate =
            (employee != null
                    && bid.getFreelancer() != null
                    && bid.getFreelancer().getId().equals(employee.getId()))
                || (freelancerEmail != null
                    && bid.getFreelancer() != null
                    && freelancerEmail.equalsIgnoreCase(bid.getFreelancer().getEmail()));
        if (isCandidate) {
          bid.setStatus(BidStatus.ACCEPTED);
          bidRepository.save(bid);
        } else if (bid.getStatus() == BidStatus.PENDING) {
          bid.setStatus(BidStatus.REJECTED);
          bidRepository.save(bid);
        }
      }
    }

    String termsFileKey = null;
    if (file != null && !file.isEmpty()) {
      termsFileKey = storageService.uploadPrivateDocument("contracts/" + UUID.randomUUID(), file);
    } else if (job.getTermsFileKey() != null && !job.getTermsFileKey().isBlank()) {
      termsFileKey = job.getTermsFileKey();
    }

    Contract contract =
        Contract.builder()
            .job(job)
            .employer(employer)
            .employee(employee)
            .freelancerEmail(freelancerEmail)
            .message(request.message())
            .sourceType("DIRECT_INVITE")
            .sourceId(job.getId())
            .salaryType(job.getSalaryType() != null ? job.getSalaryType().name() : null)
            .agreedRate(job.getBudget() != null ? job.getBudget() : job.getBudgetMin())
            .status(ContractStatus.PENDING)
            .paymentStatus("PENDING")
            .termsFileKey(termsFileKey)
            .build();

    contract = contractRepository.save(contract);

    // Publish ContractCreatedEvent with effective job fallback values
    String employerName =
        (employer.getFullName() != null && !employer.getFullName().isBlank())
            ? employer.getFullName()
            : employer.getEmail();
    com.occupify.core.event.ContractCreatedEvent event =
        new com.occupify.core.event.ContractCreatedEvent(
            UUID.randomUUID(),
            contract.getId(),
            job.getId(),
            job.getProject() != null ? job.getProject().getId() : null,
            employer.getId(),
            employee != null ? employee.getId() : null,
            freelancerEmail,
            request.message(),
            contract.getEffectiveRole(),
            contract.getEffectiveTitle(),
            contract.getEffectiveRate(),
            contract.getEffectiveSalaryType(),
            contract.getEffectiveStartedAt(),
            contract.getEffectiveFinishedAt(),
            contract.getCreatedAt() != null ? contract.getCreatedAt() : LocalDateTime.now(),
            employerName,
            employer.getEmail());
    contractEventProducer.publishContractCreated(event);

    String termsFileUrl = resolveTermsFileUrl(contract.getEffectiveTermsFileKey());
    return contractMapper.toDetailResponse(contract, termsFileUrl, null);
  }

  @Override
  public ContractDetailResponse updateContract(
      UUID id, UpdateContractRequest request, MultipartFile file, UserContext userContext) {
    log.info("[ContractService] Updating contract: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    if (!userContext.isAdmin()
        && (contract.getEmployer() == null
            || !contract.getEmployer().getId().equals(userContext.userId()))) {
      throw new ContractException(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS);
    }

    validateStatusForUpdate(contract.getStatus());

    if (request != null) {
      applyUpdateRequest(contract, request);
    }

    if (file != null && !file.isEmpty()) {
      if (contract.getTermsFileKey() != null) {
        storageService.deleteFile(contract.getTermsFileKey());
      }
      String termsFileKey =
          storageService.uploadPrivateDocument("contracts/" + contract.getId(), file);
      contract.setTermsFileKey(termsFileKey);
    }

    contract = contractRepository.save(contract);
    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    CancellationContractResponse cancellationResponse =
        resolveCancellationResponse(contract.getId());

    return contractMapper.toDetailResponse(contract, termsFileUrl, cancellationResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContractSummaryResponse> getContracts(
      ContractFilterRequest filter, Pageable pageable, UserContext userContext) {
    log.info(
        "[ContractService] Querying contracts with filter: {}, user={}",
        filter,
        userContext.userId());
    Specification<Contract> spec = ContractSpecification.withFilter(filter, userContext);
    Page<Contract> page = contractRepository.findAll(spec, pageable);
    return page.map(contractMapper::toSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ContractDetailResponse getContractDetail(UUID id, UserContext userContext) {
    log.info("[ContractService] Getting contract detail: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    verifyContractAccess(contract, userContext);

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    CancellationContractResponse cancellationResponse =
        resolveCancellationResponse(contract.getId());

    return contractMapper.toDetailResponse(contract, termsFileUrl, cancellationResponse);
  }

  @Override
  public ContractDetailResponse cancelContract(UUID id, UserContext userContext) {
    log.info("[ContractService] Cancelling contract: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    verifyContractAccess(contract, userContext);
    validateStatusForCancellation(contract.getStatus());

    User requestedByUser =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));

    CancellationContract cancellation =
        CancellationContract.builder()
            .contract(contract)
            .requestedByUser(requestedByUser)
            .reason("Contract cancelled by user")
            .status("ACCEPTED")
            .build();
    cancellation = cancellationContractRepository.save(cancellation);

    contract.setStatus(ContractStatus.CANCELLED);
    contract.setFinishedAt(LocalDateTime.now());
    contract = contractRepository.save(contract);

    expirePendingOffers(contract.getId());

    if (contract.getJob() != null) {
      jobLifecycleService.recompute(contract.getJob().getId());
    }

    String termsFileUrl = resolveTermsFileUrl(contract.getEffectiveTermsFileKey());
    String evidenceFileUrl = null;
    if (cancellation.getEvidenceFileKey() != null) {
      evidenceFileUrl =
          storageService.generatePresignedDownloadUrl(
              cancellation.getEvidenceFileKey(), PRESIGNED_URL_DURATION);
    }
    CancellationContractResponse cancellationResponse =
        contractMapper.toCancellationResponse(cancellation, evidenceFileUrl);

    return contractMapper.toDetailResponse(contract, termsFileUrl, cancellationResponse);
  }

  @Override
  public ContractDetailResponse acceptContract(UUID id, UserContext userContext) {
    log.info("[ContractService] Accepting contract: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    boolean isAssignedEmployee =
        contract.getEmployee() != null
            && contract.getEmployee().getId().equals(userContext.userId());
    boolean isAssignedEmail =
        contract.getFreelancerEmail() != null
            && contract.getFreelancerEmail().equalsIgnoreCase(userContext.email());
    if (!isAssignedEmployee && !isAssignedEmail) {
      throw new ContractException(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS);
    }

    if (contract.getEmployee() == null) {
      userRepository.findById(userContext.userId()).ifPresent(contract::setEmployee);
    }

    validateStatusForAcceptance(contract.getStatus());

    contract.setStatus(ContractStatus.PENDING_ESCROW);
    contract = contractRepository.save(contract);

    if (contract.getJob() != null) {
      List<Bid> bids = bidRepository.findByJobId(contract.getJob().getId());
      for (Bid b : bids) {
        if (b.getStatus() == BidStatus.PENDING) {
          b.setStatus(BidStatus.REJECTED);
          bidRepository.save(b);
        }
      }
    }

    UUID employerId = contract.getEmployer() != null ? contract.getEmployer().getId() : null;
    UUID jobId = contract.getJob() != null ? contract.getJob().getId() : null;
    UUID projectId =
        (contract.getJob() != null && contract.getJob().getProject() != null)
            ? contract.getJob().getProject().getId()
            : null;

    SalaryType salaryTypeEnum = SalaryType.FIXED;
    String effectiveSalaryType = contract.getEffectiveSalaryType();
    try {
      if (effectiveSalaryType != null) {
        salaryTypeEnum = SalaryType.valueOf(effectiveSalaryType.toUpperCase());
      }
    } catch (IllegalArgumentException ignored) {
    }

    String employerEmail =
        contract.getEmployer() != null ? contract.getEmployer().getEmail() : null;
    String employerName =
        (contract.getEmployer() != null && contract.getEmployer().getFullName() != null)
            ? contract.getEmployer().getFullName()
            : (employerEmail != null ? employerEmail : "Nhà tuyển dụng");
    String freelancerEmail =
        contract.getEmployee() != null ? contract.getEmployee().getEmail() : userContext.email();
    String freelancerName =
        (contract.getEmployee() != null && contract.getEmployee().getFullName() != null)
            ? contract.getEmployee().getFullName()
            : (freelancerEmail != null ? freelancerEmail : "Ứng viên");

    ContractAcceptedEvent event =
        new ContractAcceptedEvent(
            UUID.randomUUID(),
            contract.getId(),
            jobId,
            projectId,
            employerId,
            userContext.userId(),
            contract.getEffectiveTitle(),
            contract.getEffectiveRate(),
            salaryTypeEnum,
            LocalDate.now(),
            contract.getFinishedAt() != null ? contract.getFinishedAt().toLocalDate() : null,
            LocalDateTime.now(),
            employerEmail,
            employerName,
            freelancerName,
            freelancerEmail);

    contractEventProducer.publishContractAccepted(event);

    if (contract.getJob() != null) {
      jobLifecycleService.recompute(contract.getJob().getId());
    }

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    return contractMapper.toDetailResponse(contract, termsFileUrl, null);
  }

  @Override
  public ContractDetailResponse rejectContract(UUID id, UserContext userContext) {
    log.info("[ContractService] Rejecting contract: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    boolean isAssignedEmployee =
        contract.getEmployee() != null
            && contract.getEmployee().getId().equals(userContext.userId());
    boolean isAssignedEmail =
        contract.getFreelancerEmail() != null
            && contract.getFreelancerEmail().equalsIgnoreCase(userContext.email());
    if (!isAssignedEmployee && !isAssignedEmail) {
      throw new ContractException(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS);
    }

    if (contract.getEmployee() == null) {
      userRepository.findById(userContext.userId()).ifPresent(contract::setEmployee);
    }

    validateStatusForAcceptance(contract.getStatus());

    contract.setStatus(ContractStatus.CANCELLED);
    contract.setFinishedAt(LocalDateTime.now());
    contract = contractRepository.save(contract);

    rejectPendingOffers(contract.getId());

    CancellationContract cancellation =
        CancellationContract.builder()
            .contract(contract)
            .requestedByUser(contract.getEmployee())
            .reason("Contract rejected by freelancer")
            .status("ACCEPTED")
            .build();
    cancellation = cancellationContractRepository.save(cancellation);

    UUID employerId = contract.getEmployer() != null ? contract.getEmployer().getId() : null;
    UUID jobId = contract.getJob() != null ? contract.getJob().getId() : null;

    String employerEmail =
        contract.getEmployer() != null ? contract.getEmployer().getEmail() : null;
    String employerName =
        (contract.getEmployer() != null && contract.getEmployer().getFullName() != null)
            ? contract.getEmployer().getFullName()
            : (employerEmail != null ? employerEmail : "Nhà tuyển dụng");
    String freelancerEmail =
        contract.getEmployee() != null ? contract.getEmployee().getEmail() : userContext.email();
    String freelancerName =
        (contract.getEmployee() != null && contract.getEmployee().getFullName() != null)
            ? contract.getEmployee().getFullName()
            : (freelancerEmail != null ? freelancerEmail : "Ứng viên");
    String jobTitle = contract.getEffectiveTitle();

    ContractRejectedEvent event =
        new ContractRejectedEvent(
            UUID.randomUUID(),
            contract.getId(),
            jobId,
            employerId,
            userContext.userId(),
            "Contract rejected by freelancer",
            LocalDateTime.now(),
            employerEmail,
            employerName,
            freelancerName,
            jobTitle);
    contractEventProducer.publishContractRejected(event);

    String termsFileUrl = resolveTermsFileUrl(contract.getEffectiveTermsFileKey());
    CancellationContractResponse cancellationResponse =
        contractMapper.toCancellationResponse(cancellation, null);

    return contractMapper.toDetailResponse(contract, termsFileUrl, cancellationResponse);
  }

  @Override
  public ContractDetailResponse completeContract(
      UUID id, CompleteContractRequest request, UserContext userContext) {
    log.info("[ContractService] Completing contract: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    if (contract.getEmployer() == null
        || !contract.getEmployer().getId().equals(userContext.userId())) {
      throw new ContractException(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS);
    }

    validateStatusForCompletion(contract.getStatus());

    contract.setStatus(ContractStatus.COMPLETED);
    contract.setFinishedAt(LocalDateTime.now());
    contract = contractRepository.save(contract);

    Review review =
        Review.builder()
            .contract(contract)
            .reviewer(contract.getEmployer())
            .reviewee(contract.getEmployee())
            .rating(request.rating())
            .comment(request.comment())
            .build();
    reviewRepository.save(review);

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    return contractMapper.toDetailResponse(contract, termsFileUrl, null);
  }

  @Override
  public void removeContract(UUID id, UserContext userContext) {
    log.info("[ContractService] Removing contract: id={}, user={}", id, userContext.userId());

    Contract contract = getActiveContractOrThrow(id);

    verifyContractAccess(contract, userContext);

    contract.setDeletedAt(LocalDateTime.now());
    if (contract.getStatus() == ContractStatus.PENDING) {
      contract.setStatus(ContractStatus.CANCELLED);
    }
    contractRepository.save(contract);

    rejectPendingOffers(contract.getId());

    if (contract.getJob() != null) {
      List<Bid> bids = bidRepository.findByJobId(contract.getJob().getId());
      for (Bid b : bids) {
        if (b.getStatus() == BidStatus.PENDING) {
          b.setStatus(BidStatus.REJECTED);
          bidRepository.save(b);
        }
      }
    }
  }

  private User resolveEmployee(CreateContractRequest request) {
    if (request.freelancerGmail() != null && !request.freelancerGmail().isBlank()) {
      return userRepository
          .findByEmail(request.freelancerGmail().trim())
          .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));
    }
    throw new ContractException(ContractErrorCode.USER_NOT_FOUND);
  }

  private void applyUpdateRequest(Contract contract, UpdateContractRequest request) {
    if (request.jobId() != null
        && (contract.getJob() == null || !contract.getJob().getId().equals(request.jobId()))) {
      Job job =
          jobRepository
              .findById(request.jobId())
              .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_FOUND));
      contract.setJob(job);
    }
    if (request.message() != null) {
      contract.setMessage(request.message());
    }
    if (request.freelancerGmail() != null && !request.freelancerGmail().isBlank()) {
      String email = request.freelancerGmail().trim();
      contract.setFreelancerEmail(email);
      User employee =
          userRepository
              .findByEmail(email)
              .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));
      contract.setEmployee(employee);
    }
  }

  private Contract getActiveContractOrThrow(UUID id) {
    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));
    if (contract.getDeletedAt() != null) {
      throw new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND);
    }
    return contract;
  }

  private void validateStatusForUpdate(ContractStatus status) {
    if (status == ContractStatus.COMPLETED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_COMPLETED);
    }
    if (status == ContractStatus.CANCELLED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_CANCELLED);
    }
    if (status != ContractStatus.PENDING) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_ACTIVE);
    }
  }

  private void validateStatusForCancellation(ContractStatus status) {
    if (status == ContractStatus.COMPLETED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_COMPLETED);
    }
    if (status == ContractStatus.CANCELLED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_CANCELLED);
    }
  }

  private void validateStatusForAcceptance(ContractStatus status) {
    if (status == ContractStatus.COMPLETED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_COMPLETED);
    }
    if (status == ContractStatus.CANCELLED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_CANCELLED);
    }
    if (status != ContractStatus.PENDING) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_ACTIVE);
    }
  }

  private void validateStatusForCompletion(ContractStatus status) {
    if (status == ContractStatus.COMPLETED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_COMPLETED);
    }
    if (status == ContractStatus.CANCELLED) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_CANCELLED);
    }
    if (status != ContractStatus.IN_PROGRESS) {
      throw new ContractException(ContractErrorCode.CONTRACT_ALREADY_ACTIVE);
    }
  }

  private void expirePendingOffers(UUID contractId) {
    List<Offer> offers = offerRepository.findByContractId(contractId);
    for (Offer offer : offers) {
      if (offer.getStatus() == OfferStatus.PENDING) {
        offer.setStatus(OfferStatus.EXPIRED);
        offerRepository.save(offer);
      }
    }
  }

  private void acceptPendingOffers(UUID contractId) {
    List<Offer> offers = offerRepository.findByContractId(contractId);
    for (Offer offer : offers) {
      if (offer.getStatus() == OfferStatus.PENDING) {
        offer.setStatus(OfferStatus.ACCEPTED);
        offerRepository.save(offer);
      }
    }
  }

  private void rejectPendingOffers(UUID contractId) {
    List<Offer> offers = offerRepository.findByContractId(contractId);
    for (Offer offer : offers) {
      if (offer.getStatus() == OfferStatus.PENDING) {
        offer.setStatus(OfferStatus.REJECTED);
        offerRepository.save(offer);
      }
    }
  }

  private void verifyContractAccess(Contract contract, UserContext userContext) {
    if (userContext.isAdmin()) {
      return;
    }
    boolean isEmployer =
        contract.getEmployer() != null
            && contract.getEmployer().getId().equals(userContext.userId());
    boolean isEmployee =
        contract.getEmployee() != null
            && contract.getEmployee().getId().equals(userContext.userId());
    if (!isEmployer && !isEmployee) {
      throw new ContractException(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS);
    }
  }

  private String resolveTermsFileUrl(String termsFileKey) {
    if (termsFileKey == null || termsFileKey.isBlank()) {
      return null;
    }
    return storageService.generatePresignedDownloadUrl(termsFileKey, PRESIGNED_URL_DURATION);
  }

  private CancellationContractResponse resolveCancellationResponse(UUID contractId) {
    return cancellationContractRepository
        .findFirstByContractIdOrderByCreatedAtDesc(contractId)
        .map(
            cancellation -> {
              String evidenceUrl = null;
              if (cancellation.getEvidenceFileKey() != null
                  && !cancellation.getEvidenceFileKey().isBlank()) {
                evidenceUrl =
                    storageService.generatePresignedDownloadUrl(
                        cancellation.getEvidenceFileKey(), PRESIGNED_URL_DURATION);
              }
              return contractMapper.toCancellationResponse(cancellation, evidenceUrl);
            })
        .orElse(null);
  }

  @Override
  public void confirmContractEscrow(UUID contractId) {
    Contract contract = contractRepository.findById(contractId).orElse(null);
    if (contract == null) {
      log.error("Contract [{}] not found for activation", contractId);
      return;
    }

    if (contract.getStatus() == ContractStatus.IN_PROGRESS) {
      log.info("Contract [{}] is already IN_PROGRESS", contractId);
      return;
    }

    if (contract.getStatus() == ContractStatus.PENDING_ESCROW) {
      contract.setStatus(ContractStatus.IN_PROGRESS);
      contract.setStartedAt(LocalDateTime.now());
      contractRepository.save(contract);
      acceptPendingOffers(contract.getId());
    } else {
      log.warn(
          "Contract [{}] has status [{}], expected PENDING_ESCROW for activation",
          contractId,
          contract.getStatus());
    }
  }

  @Override
  public void compensateContract(UUID contractId, String reason) {
    Contract contract = contractRepository.findById(contractId).orElse(null);
    if (contract == null) {
      log.error("Contract [{}] not found for rollback", contractId);
      return;
    }

    if (contract.getStatus() == ContractStatus.IN_PROGRESS
        || contract.getStatus() == ContractStatus.PENDING_ESCROW) {
      log.error(
          "Rolling back contract [{}] from [{}] to PENDING. Reason: {}",
          contractId,
          contract.getStatus(),
          reason);
      contract.setStatus(ContractStatus.PENDING);
      contract.setStartedAt(null);
      contractRepository.save(contract);
    }
  }
}
