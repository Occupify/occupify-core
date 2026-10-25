package com.occupify.core.service.impl;

import com.occupify.core.dto.request.contract.CancelContractRequest;
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
import com.occupify.core.enums.OfferStatus;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.ContractMapper;
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
import com.occupify.core.service.storage.StorageService;
import java.time.Duration;
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

  @Override
  public ContractDetailResponse createContract(
      CreateContractRequest request, MultipartFile file, UserContext userContext) {
    log.info(
        "[ContractService] Creating contract: title='{}', user={}",
        request.title(),
        userContext.userId());

    User employer =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));

    Job job =
        jobRepository
            .findById(request.jobId())
            .orElseThrow(() -> new ContractException(ContractErrorCode.JOB_NOT_OWNED));

    if (job.getProject() == null
        || job.getProject().getEmployer() == null
        || !job.getProject().getEmployer().getId().equals(employer.getId())) {
      throw new ContractException(ContractErrorCode.JOB_NOT_OWNED);
    }

    User employee = resolveEmployee(request);

    if ("JOB_BID".equalsIgnoreCase(request.sourceType()) && request.sourceId() != null) {
      cascadeAcceptedBid(job.getId(), request.sourceId());
    }

    String termsFileKey = null;
    if (file != null && !file.isEmpty()) {
      termsFileKey = storageService.uploadPrivateDocument("contracts/" + UUID.randomUUID(), file);
    }

    Contract contract =
        Contract.builder()
            .job(job)
            .employer(employer)
            .employee(employee)
            .title(request.title())
            .sourceType(request.sourceType())
            .sourceId(request.sourceId())
            .agreedRate(request.agreedRate())
            .salaryType(request.salaryType())
            .salaryDueDate(request.salaryDueDate())
            .jobDescription(request.jobDescription())
            .status(ContractStatus.PENDING)
            .termsFileKey(termsFileKey)
            .startedAt(request.startedAt())
            .finishedAt(request.finishedAt())
            .build();

    contract = contractRepository.save(contract);

    Offer offer =
        Offer.builder()
            .contract(contract)
            .freelancer(employee)
            .proposedRate(request.agreedRate())
            .offerMessage(request.invitationMessage())
            .status(OfferStatus.PENDING)
            .build();
    offerRepository.save(offer);

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    return contractMapper.toDetailResponse(contract, termsFileUrl, null);
  }

  @Override
  public ContractDetailResponse updateContract(
      UUID id, UpdateContractRequest request, MultipartFile file, UserContext userContext) {
    log.info("[ContractService] Updating contract: id={}, user={}", id, userContext.userId());

    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

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

    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

    verifyContractAccess(contract, userContext);

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    CancellationContractResponse cancellationResponse =
        resolveCancellationResponse(contract.getId());

    return contractMapper.toDetailResponse(contract, termsFileUrl, cancellationResponse);
  }

  @Override
  public ContractDetailResponse cancelContract(
      UUID id, CancelContractRequest request, MultipartFile file, UserContext userContext) {
    log.info("[ContractService] Cancelling contract: id={}, user={}", id, userContext.userId());

    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

    verifyContractAccess(contract, userContext);
    validateStatusForCancellation(contract.getStatus());

    if (request == null
        || request.reason() == null
        || request.reason().trim().length() < MIN_REASON_LENGTH) {
      throw new ContractException(ContractErrorCode.INVALID_CANCELLATION_REASON);
    }

    User requestedByUser =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));

    String evidenceFileKey = null;
    if (file != null && !file.isEmpty()) {
      evidenceFileKey =
          storageService.uploadPrivateDocument("cancellations/" + contract.getId(), file);
    }

    CancellationContract cancellation =
        CancellationContract.builder()
            .contract(contract)
            .requestedByUser(requestedByUser)
            .reason(request.reason().trim())
            .evidenceFileKey(evidenceFileKey)
            .status("ACCEPTED")
            .build();
    cancellation = cancellationContractRepository.save(cancellation);

    contract.setStatus(ContractStatus.CANCELLED);
    contract = contractRepository.save(contract);

    expirePendingOffers(contract.getId());

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
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

    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

    if (contract.getEmployee() == null
        || !contract.getEmployee().getId().equals(userContext.userId())) {
      throw new ContractException(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS);
    }

    validateStatusForAcceptance(contract.getStatus());

    contract.setStatus(ContractStatus.IN_PROGRESS);
    contract.setStartedAt(LocalDateTime.now());
    contract = contractRepository.save(contract);

    acceptPendingOffers(contract.getId());

    String termsFileUrl = resolveTermsFileUrl(contract.getTermsFileKey());
    return contractMapper.toDetailResponse(contract, termsFileUrl, null);
  }

  @Override
  public ContractDetailResponse completeContract(
      UUID id, CompleteContractRequest request, UserContext userContext) {
    log.info("[ContractService] Completing contract: id={}, user={}", id, userContext.userId());

    Contract contract =
        contractRepository
            .findById(id)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

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

  private User resolveEmployee(CreateContractRequest request) {
    if (request.employeeId() != null) {
      return userRepository
          .findById(request.employeeId())
          .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));
    }
    if (request.freelancerEmail() != null && !request.freelancerEmail().isBlank()) {
      return userRepository
          .findByEmail(request.freelancerEmail().trim())
          .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));
    }
    throw new ContractException(ContractErrorCode.USER_NOT_FOUND);
  }

  private void cascadeAcceptedBid(UUID jobId, UUID bidId) {
    Bid bid =
        bidRepository
            .findById(bidId)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));
    if (!bid.getJob().getId().equals(jobId)) {
      throw new ContractException(ContractErrorCode.JOB_NOT_OWNED);
    }
    bid.setStatus(BidStatus.ACCEPTED);
    bidRepository.save(bid);
  }

  private void applyUpdateRequest(Contract contract, UpdateContractRequest request) {
    if (request.title() != null) {
      contract.setTitle(request.title());
    }
    if (request.agreedRate() != null) {
      contract.setAgreedRate(request.agreedRate());
    }
    if (request.salaryType() != null) {
      contract.setSalaryType(request.salaryType());
    }
    if (request.salaryDueDate() != null) {
      contract.setSalaryDueDate(request.salaryDueDate());
    }
    if (request.startedAt() != null) {
      contract.setStartedAt(request.startedAt());
    }
    if (request.finishedAt() != null) {
      contract.setFinishedAt(request.finishedAt());
    }
    if (request.jobDescription() != null) {
      contract.setJobDescription(request.jobDescription());
    }
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
}
