package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.review.Review;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.OfferStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.ContractMapper;
import com.occupify.core.repository.bid.BidRepository;
import com.occupify.core.repository.contract.CancellationContractRepository;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.offer.OfferRepository;
import com.occupify.core.repository.review.ReviewRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.impl.ContractServiceImpl;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

class ContractServiceTest {

  private ContractRepository contractRepository;
  private CancellationContractRepository cancellationContractRepository;
  private JobRepository jobRepository;
  private UserRepository userRepository;
  private BidRepository bidRepository;
  private OfferRepository offerRepository;
  private ReviewRepository reviewRepository;
  private StorageService storageService;
  private ContractMapper contractMapper;
  private com.occupify.core.producer.ContractEventProducer contractEventProducer;
  private JobLifecycleService jobLifecycleService;

  private ContractService contractService;

  private UUID employerId;
  private UserContext employerContext;
  private User employer;
  private UUID employeeId;
  private UserContext employeeContext;
  private User employee;
  private Job job;

  @BeforeEach
  void setUp() {
    contractRepository = mock(ContractRepository.class);
    cancellationContractRepository = mock(CancellationContractRepository.class);
    jobRepository = mock(JobRepository.class);
    userRepository = mock(UserRepository.class);
    bidRepository = mock(BidRepository.class);
    offerRepository = mock(OfferRepository.class);
    reviewRepository = mock(ReviewRepository.class);
    storageService = mock(StorageService.class);
    contractMapper = mock(ContractMapper.class);
    contractEventProducer = mock(com.occupify.core.producer.ContractEventProducer.class);
    jobLifecycleService = mock(JobLifecycleService.class);

    contractService =
        new ContractServiceImpl(
            contractRepository,
            cancellationContractRepository,
            jobRepository,
            userRepository,
            bidRepository,
            offerRepository,
            reviewRepository,
            storageService,
            contractMapper,
            contractEventProducer,
            jobLifecycleService);

    employerId = UUID.randomUUID();
    employerContext = new UserContext(employerId, UserRole.USER, "employer@test.local");
    employer = User.builder().email("employer@test.local").fullName("Employer Corp").build();
    employer.setId(employerId);

    employeeId = UUID.randomUUID();
    employeeContext = new UserContext(employeeId, UserRole.USER, "freelancer@test.local");
    employee = User.builder().email("freelancer@test.local").fullName("Freelancer Pro").build();
    employee.setId(employeeId);

    Project project = Project.builder().employer(employer).title("Project A").build();
    job = Job.builder().project(project).title("Job Role").build();
    job.setId(UUID.randomUUID());
  }

  @Test
  @DisplayName(
      "Should create contract from Bid, cascade bid status to ACCEPTED and not create Offer")
  void shouldCreateContractFromBid() {
    UUID bidId = UUID.randomUUID();
    Bid bid = Bid.builder().job(job).freelancer(employee).status(BidStatus.PENDING).build();
    bid.setId(bidId);

    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(job.getId())
            .freelancerGmail(employee.getEmail())
            .message("Welcome to the team")
            .build();

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
    when(bidRepository.findByJobId(job.getId())).thenReturn(List.of(bid));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().title("Contract Backend Dev").build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response =
        contractService.createContract(request, null, employerContext);

    assertThat(response.getTitle()).isEqualTo("Contract Backend Dev");
    assertThat(bid.getStatus()).isEqualTo(BidStatus.ACCEPTED);
    verify(bidRepository).save(bid);
    verify(offerRepository, never()).save(any());

    ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
    verify(contractRepository).save(contractCaptor.capture());
    Contract savedContract = contractCaptor.getValue();
    assertThat(savedContract.getTermsFileKey()).isNull();
    assertThat(savedContract.getStatus()).isEqualTo(ContractStatus.PENDING);
  }

  @Test
  @DisplayName("Should create contract from Direct Offer, create Offer and link sourceId")
  void shouldCreateContractFromDirectOffer() {
    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(job.getId())
            .freelancerGmail(employee.getEmail())
            .message("Direct offer invitation")
            .build();

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().title(job.getTitle()).build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response =
        contractService.createContract(request, null, employerContext);

    assertThat(response.getTitle()).isEqualTo(job.getTitle());

    ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
    verify(contractRepository).save(contractCaptor.capture());
    Contract savedContract = contractCaptor.getValue();
    assertThat(savedContract.getFreelancerEmail()).isEqualTo(employee.getEmail());
    assertThat(savedContract.getMessage()).isEqualTo("Direct offer invitation");
    assertThat(savedContract.getSourceType()).isEqualTo("DIRECT_INVITE");
  }

  @Test
  @DisplayName("Should upload terms file to S3 when provided during contract creation")
  void shouldUploadTermsFileWhenProvided() {
    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(job.getId())
            .freelancerGmail("freelancer@test.local")
            .message("Contract Backend Dev")
            .build();

    MockMultipartFile file =
        new MockMultipartFile("file", "terms.pdf", "application/pdf", "Terms data".getBytes());

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    when(userRepository.findByEmail("freelancer@test.local")).thenReturn(Optional.of(employee));
    when(storageService.uploadPrivateDocument(any(), eq(file)))
        .thenReturn("contracts/terms-key.pdf");
    when(storageService.generatePresignedDownloadUrl(
            "contracts/terms-key.pdf", Duration.ofMinutes(15)))
        .thenReturn("https://s3.download/terms-key.pdf");
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder()
            .title("Contract Backend Dev")
            .termsFileUrl("https://s3.download/terms-key.pdf")
            .build();
    when(contractMapper.toDetailResponse(any(), eq("https://s3.download/terms-key.pdf"), any()))
        .thenReturn(mockResponse);

    ContractDetailResponse response =
        contractService.createContract(request, file, employerContext);

    assertThat(response.getTermsFileUrl()).isEqualTo("https://s3.download/terms-key.pdf");
    verify(storageService).uploadPrivateDocument(any(), eq(file));
  }

  @Test
  @DisplayName("Should reject contract update if status is IN_PROGRESS")
  void shouldRejectUpdateWhenActive() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.IN_PROGRESS)
            .build();

    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    assertThatThrownBy(
            () -> contractService.updateContract(contractId, null, null, employerContext))
        .isInstanceOf(ContractException.class)
        .hasMessageContaining("chờ duyệt");
  }

  @Test
  @DisplayName("Should successfully update contract when status is PENDING")
  void shouldUpdateContractWhenPending() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .title("Old Title")
            .agreedRate(new BigDecimal("20000000.00"))
            .salaryType("MONTHLY")
            .status(ContractStatus.PENDING)
            .build();
    contract.setId(UUID.randomUUID());

    UpdateContractRequest updateReq =
        UpdateContractRequest.builder()
            .jobId(job.getId())
            .message("Updated message")
            .freelancerGmail("dev@occupify.local")
            .build();

    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(userRepository.findByEmail("dev@occupify.local")).thenReturn(Optional.of(employee));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().title("Contract Backend Dev").build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response =
        contractService.updateContract(contract.getId(), updateReq, null, employerContext);

    assertThat(response.getTitle()).isEqualTo("Contract Backend Dev");
    assertThat(contract.getMessage()).isEqualTo("Updated message");
    assertThat(contract.getFreelancerEmail()).isEqualTo("dev@occupify.local");
  }

  @Test
  @DisplayName(
      "Should delete old terms file from S3 when new terms file is uploaded during contract update")
  void shouldDeleteOldTermsFileOnUpdate() {
    String oldFileKey = "contracts/123/old-terms.pdf";
    String newFileKey = "contracts/123/new-terms.pdf";

    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .title("Contract Title")
            .agreedRate(new BigDecimal("20000000.00"))
            .salaryType("MONTHLY")
            .status(ContractStatus.PENDING)
            .termsFileKey(oldFileKey)
            .build();
    contract.setId(UUID.randomUUID());

    MockMultipartFile newFile =
        new MockMultipartFile("file", "new-terms.pdf", "application/pdf", "new content".getBytes());

    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));
    when(storageService.uploadPrivateDocument(eq("contracts/" + contract.getId()), eq(newFile)))
        .thenReturn(newFileKey);

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().title("Contract Title").build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    contractService.updateContract(contract.getId(), null, newFile, employerContext);

    verify(storageService).deleteFile(oldFileKey);
    verify(storageService).uploadPrivateDocument(eq("contracts/" + contract.getId()), eq(newFile));
    assertThat(contract.getTermsFileKey()).isEqualTo(newFileKey);
  }

  @Test
  @DisplayName("Should reject contract cancellation if already COMPLETED")
  void shouldRejectCancellationWhenCompleted() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.COMPLETED)
            .build();

    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    assertThatThrownBy(() -> contractService.cancelContract(contractId, employerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex)
                    .getCode()
                    .equals(ContractErrorCode.CONTRACT_ALREADY_COMPLETED.code()));
  }

  @Test
  @DisplayName("Should reject contract cancellation if already CANCELLED")
  void shouldRejectCancellationWhenAlreadyCancelled() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.CANCELLED)
            .build();

    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    assertThatThrownBy(() -> contractService.cancelContract(contractId, employerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex)
                    .getCode()
                    .equals(ContractErrorCode.CONTRACT_ALREADY_CANCELLED.code()));
  }

  @Test
  @DisplayName("Should successfully cancel contract and expire pending offers")
  void shouldCancelContractAndExpireOffers() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.IN_PROGRESS)
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    Offer pendingOffer =
        Offer.builder().contract(contract).freelancer(employee).status(OfferStatus.PENDING).build();

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(offerRepository.findByContractId(contractId)).thenReturn(List.of(pendingOffer));
    when(cancellationContractRepository.save(any(CancellationContract.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().status(ContractStatus.CANCELLED).build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response = contractService.cancelContract(contractId, employerContext);

    assertThat(response.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    assertThat(pendingOffer.getStatus()).isEqualTo(OfferStatus.EXPIRED);
    verify(offerRepository).save(pendingOffer);
    verify(cancellationContractRepository).save(any(CancellationContract.class));
  }

  @Test
  @DisplayName(
      "Should accept contract, set PENDING_ESCROW, publish event, and confirm escrow sets IN_PROGRESS")
  void shouldAcceptContract() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.PENDING)
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    Offer pendingOffer =
        Offer.builder().contract(contract).freelancer(employee).status(OfferStatus.PENDING).build();

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(offerRepository.findByContractId(contractId)).thenReturn(List.of(pendingOffer));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().status(ContractStatus.PENDING_ESCROW).build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response = contractService.acceptContract(contractId, employeeContext);

    assertThat(response.getStatus()).isEqualTo(ContractStatus.PENDING_ESCROW);
    assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING_ESCROW);
    verify(contractEventProducer).publishContractAccepted(any());

    // Confirm escrow triggers activation to IN_PROGRESS
    contractService.confirmContractEscrow(contractId);
    assertThat(contract.getStatus()).isEqualTo(ContractStatus.IN_PROGRESS);
    assertThat(contract.getStartedAt()).isNotNull();
    assertThat(pendingOffer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
    verify(offerRepository).save(pendingOffer);
  }

  @Test
  @DisplayName("Should compensate contract from PENDING_ESCROW or IN_PROGRESS back to PENDING")
  void shouldCompensateContract() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.PENDING_ESCROW)
            .startedAt(LocalDateTime.now())
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    contractService.compensateContract(contractId, "PAYMENT_FAILED");

    assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING);
    assertThat(contract.getStartedAt()).isNull();
  }

  @Test
  @DisplayName("Should reject contract, set CANCELLED, reject offer, and save cancellation")
  void shouldRejectContract() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.PENDING)
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    Offer pendingOffer =
        Offer.builder().contract(contract).freelancer(employee).status(OfferStatus.PENDING).build();

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(offerRepository.findByContractId(contractId)).thenReturn(List.of(pendingOffer));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().status(ContractStatus.CANCELLED).build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response = contractService.rejectContract(contractId, employeeContext);

    assertThat(response.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    assertThat(pendingOffer.getStatus()).isEqualTo(OfferStatus.REJECTED);
    verify(offerRepository).save(pendingOffer);
    verify(cancellationContractRepository).save(any(CancellationContract.class));
  }

  @Test
  @DisplayName("Should complete contract, set COMPLETED, finishedAt, and create Review")
  void shouldCompleteContract() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.IN_PROGRESS)
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().status(ContractStatus.COMPLETED).build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    CompleteContractRequest completeReq =
        CompleteContractRequest.builder().rating(5).comment("Làm việc rất xuất sắc!").build();

    ContractDetailResponse response =
        contractService.completeContract(contractId, completeReq, employerContext);

    assertThat(response.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    assertThat(contract.getFinishedAt()).isNotNull();

    ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
    verify(reviewRepository).save(reviewCaptor.capture());
    Review savedReview = reviewCaptor.getValue();
    assertThat(savedReview.getRating()).isEqualTo(5);
    assertThat(savedReview.getComment()).isEqualTo("Làm việc rất xuất sắc!");
    assertThat(savedReview.getReviewer()).isEqualTo(employer);
    assertThat(savedReview.getReviewee()).isEqualTo(employee);
    assertThat(savedReview.getContract()).isEqualTo(contract);
  }

  @Test
  @DisplayName("Should retrieve contract detail with presigned URL and check access")
  void shouldGetContractDetail() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .termsFileKey("contracts/terms.pdf")
            .status(ContractStatus.IN_PROGRESS)
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(storageService.generatePresignedDownloadUrl("contracts/terms.pdf", Duration.ofMinutes(15)))
        .thenReturn("https://s3.download/terms.pdf");
    when(cancellationContractRepository.findFirstByContractIdOrderByCreatedAtDesc(contractId))
        .thenReturn(Optional.empty());

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder()
            .id(contractId)
            .termsFileUrl("https://s3.download/terms.pdf")
            .build();
    when(contractMapper.toDetailResponse(
            eq(contract), eq("https://s3.download/terms.pdf"), isNull()))
        .thenReturn(mockResponse);

    ContractDetailResponse response =
        contractService.getContractDetail(contractId, employerContext);

    assertThat(response.getId()).isEqualTo(contractId);
    assertThat(response.getTermsFileUrl()).isEqualTo("https://s3.download/terms.pdf");

    // Check third party access forbidden
    UserContext strangerContext =
        new UserContext(UUID.randomUUID(), UserRole.USER, "stranger@test.local");
    assertThatThrownBy(() -> contractService.getContractDetail(contractId, strangerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex)
                    .getCode()
                    .equals(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS.code()));
  }

  @Test
  @DisplayName("Should scope getContracts to current user when not admin")
  @SuppressWarnings("unchecked")
  void shouldScopeGetContractsToCurrentUser() {
    Contract contract = Contract.builder().employer(employer).employee(employee).job(job).build();
    Page<Contract> contractPage = new PageImpl<>(List.of(contract));

    when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(contractPage);

    ContractSummaryResponse summaryResponse =
        ContractSummaryResponse.builder().id(UUID.randomUUID()).title("Summary").build();
    when(contractMapper.toSummaryResponse(contract)).thenReturn(summaryResponse);

    ContractFilterRequest filter =
        ContractFilterRequest.builder().status(ContractStatus.IN_PROGRESS).build();
    Pageable pageable = PageRequest.of(0, 10);

    Page<ContractSummaryResponse> result =
        contractService.getContracts(filter, pageable, employerContext);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("Summary");
  }

  @Test
  @DisplayName("Should reject contract creation if job does not belong to employer's project")
  void shouldRejectCreateContractWhenJobNotOwnedByEmployer() {
    User otherEmployer = User.builder().email("other@test.local").build();
    otherEmployer.setId(UUID.randomUUID());
    Project otherProject = Project.builder().employer(otherEmployer).title("Other").build();
    Job unownedJob = Job.builder().project(otherProject).title("Other Job").build();
    unownedJob.setId(UUID.randomUUID());

    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(unownedJob.getId())
            .freelancerGmail("dev@test.local")
            .message("Dev")
            .build();

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findById(unownedJob.getId())).thenReturn(Optional.of(unownedJob));

    assertThatThrownBy(() -> contractService.createContract(request, null, employerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex).getCode().equals(ContractErrorCode.JOB_NOT_OWNED.code()));
  }

  @Test
  @DisplayName(
      "Should throw USER_NOT_FOUND when freelancer email does not exist in DB on contract creation")
  void shouldThrowUserNotFoundWhenFreelancerDoesNotExistInDbOnCreate() {
    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(job.getId())
            .freelancerGmail("unknown-freelancer@test.local")
            .message("Dev")
            .build();

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    when(userRepository.findByEmail("unknown-freelancer@test.local")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> contractService.createContract(request, null, employerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex).getCode().equals(ContractErrorCode.USER_NOT_FOUND.code()));
  }

  @Test
  @DisplayName("Should throw USER_NOT_FOUND when updating to non-existent freelancer email")
  void shouldThrowUserNotFoundWhenUpdatingToNonExistentFreelancerEmail() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.PENDING)
            .build();
    contract.setId(UUID.randomUUID());

    UpdateContractRequest request =
        UpdateContractRequest.builder().freelancerGmail("nonexistent@test.local").build();

    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(userRepository.findByEmail("nonexistent@test.local")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> contractService.updateContract(contract.getId(), request, null, employerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex).getCode().equals(ContractErrorCode.USER_NOT_FOUND.code()));
  }

  @Test
  @DisplayName("Should reject acceptContract when user is not the employee")
  void shouldRejectAcceptContractWhenUserIsNotEmployee() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.PENDING)
            .build();
    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    assertThatThrownBy(() -> contractService.acceptContract(contractId, employerContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex)
                    .getCode()
                    .equals(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS.code()));
  }

  @Test
  @DisplayName("Should reject completeContract when user is not the employer")
  void shouldRejectCompleteContractWhenUserIsNotEmployer() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.IN_PROGRESS)
            .build();
    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    CompleteContractRequest completeReq =
        CompleteContractRequest.builder().rating(5).comment("Good").build();

    assertThatThrownBy(
            () -> contractService.completeContract(contractId, completeReq, employeeContext))
        .isInstanceOf(ContractException.class)
        .matches(
            ex ->
                ((ContractException) ex)
                    .getCode()
                    .equals(ContractErrorCode.FORBIDDEN_CONTRACT_ACCESS.code()));
  }

  @Test
  @DisplayName("Should get contract detail with cancellation evidence URL when cancelled")
  void shouldGetContractDetailWithCancellationEvidenceUrl() {
    Contract contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .job(job)
            .status(ContractStatus.CANCELLED)
            .build();
    UUID contractId = UUID.randomUUID();
    contract.setId(contractId);

    CancellationContract cancellation =
        CancellationContract.builder()
            .contract(contract)
            .requestedByUser(employer)
            .reason("Reason why cancelled")
            .evidenceFileKey("cancellations/proof.pdf")
            .status("ACCEPTED")
            .build();

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(cancellationContractRepository.findFirstByContractIdOrderByCreatedAtDesc(contractId))
        .thenReturn(Optional.of(cancellation));
    when(storageService.generatePresignedDownloadUrl(
            "cancellations/proof.pdf", Duration.ofMinutes(15)))
        .thenReturn("https://s3.download/proof.pdf");

    CancellationContractResponse cancellationResponse =
        CancellationContractResponse.builder()
            .reason("Reason why cancelled")
            .evidenceFileUrl("https://s3.download/proof.pdf")
            .build();
    when(contractMapper.toCancellationResponse(cancellation, "https://s3.download/proof.pdf"))
        .thenReturn(cancellationResponse);

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().id(contractId).cancellation(cancellationResponse).build();
    when(contractMapper.toDetailResponse(contract, null, cancellationResponse))
        .thenReturn(mockResponse);

    ContractDetailResponse response =
        contractService.getContractDetail(contractId, employerContext);

    assertThat(response.getCancellation()).isNotNull();
    assertThat(response.getCancellation().getEvidenceFileUrl())
        .isEqualTo("https://s3.download/proof.pdf");
  }

  @Test
  @DisplayName("Should auto-close Job and publish ContractCreatedEvent when contract is created")
  void shouldAutoCloseJobAndPublishEventOnContractCreate() {
    job.setStatus(com.occupify.core.enums.JobStatus.RECRUITING);

    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(job.getId())
            .freelancerGmail("freelancer@test.local")
            .message("Please join the project")
            .build();

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findWithProjectById(job.getId())).thenReturn(Optional.of(job));
    when(userRepository.findByEmail("freelancer@test.local")).thenReturn(Optional.of(employee));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse =
        ContractDetailResponse.builder().id(UUID.randomUUID()).build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    contractService.createContract(request, null, employerContext);

    // Verify job is closed
    assertThat(job.getStatus()).isEqualTo(com.occupify.core.enums.JobStatus.CLOSED);
    verify(jobRepository).save(job);

    // Verify ContractCreatedEvent published
    verify(contractEventProducer)
        .publishContractCreated(any(com.occupify.core.event.ContractCreatedEvent.class));
  }

  @Test
  @DisplayName("Should soft delete and cancel contract on removeContract")
  void shouldSoftDeleteContractOnRemoveContract() {
    UUID contractId = UUID.randomUUID();
    Contract contract =
        Contract.builder()
            .job(job)
            .employer(employer)
            .employee(employee)
            .status(ContractStatus.PENDING)
            .build();
    contract.setId(contractId);

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    contractService.removeContract(contractId, employerContext);

    assertThat(contract.getDeletedAt()).isNotNull();
    assertThat(contract.getStatus()).isEqualTo(ContractStatus.CANCELLED);
    verify(contractRepository).save(contract);
  }
}
