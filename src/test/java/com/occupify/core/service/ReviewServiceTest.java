package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.dto.request.review.CreateReviewRequest;
import com.occupify.core.dto.response.review.ReviewResponse;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.review.Review;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.event.domain.ReviewCreatedDomainEvent;
import com.occupify.core.exception.review.ReviewErrorCode;
import com.occupify.core.exception.review.ReviewException;
import com.occupify.core.mapper.ReviewMapper;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.review.ReviewRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.impl.ReviewServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ReviewServiceTest {

  private ReviewRepository reviewRepository;
  private ContractRepository contractRepository;
  private UserRepository userRepository;
  private ReviewMapper reviewMapper;
  private ContractEventProducer contractEventProducer;
  private ApplicationEventPublisher applicationEventPublisher;
  private ReviewServiceImpl reviewService;

  private UUID employerId;
  private UUID employeeId;
  private UUID strangerId;
  private UUID contractId;
  private User employer;
  private User employee;
  private Contract contract;

  @BeforeEach
  void setUp() {
    reviewRepository = mock(ReviewRepository.class);
    contractRepository = mock(ContractRepository.class);
    userRepository = mock(UserRepository.class);
    reviewMapper = mock(ReviewMapper.class);
    contractEventProducer = mock(ContractEventProducer.class);
    applicationEventPublisher = mock(ApplicationEventPublisher.class);

    reviewService =
        new ReviewServiceImpl(
            reviewRepository,
            contractRepository,
            userRepository,
            reviewMapper,
            contractEventProducer,
            applicationEventPublisher);

    employerId = UUID.randomUUID();
    employeeId = UUID.randomUUID();
    strangerId = UUID.randomUUID();
    contractId = UUID.randomUUID();

    employer = User.builder().email("employer@test.local").fullName("Employer Corp").build();
    employer.setId(employerId);

    employee = User.builder().email("employee@test.local").fullName("Freelancer Pro").build();
    employee.setId(employeeId);

    contract =
        Contract.builder()
            .employer(employer)
            .employee(employee)
            .status(ContractStatus.COMPLETED)
            .build();
    contract.setId(contractId);
  }

  @Test
  @DisplayName(
      "Should create review and publish ReviewCreatedDomainEvent via ApplicationEventPublisher")
  void shouldCreateReviewAndPublishDomainEvent() {
    CreateReviewRequest request = new CreateReviewRequest(contractId, 5, "Excellent work");
    UserContext userContext = new UserContext(employerId, UserRole.USER, "employer@test.local");

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
    when(reviewRepository.existsByContractIdAndReviewerId(contractId, employerId))
        .thenReturn(false);

    Review savedReview =
        Review.builder()
            .contract(contract)
            .reviewer(employer)
            .reviewee(employee)
            .rating(5)
            .comment("Excellent work")
            .build();
    savedReview.setId(UUID.randomUUID());
    when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

    ReviewResponse mockResponse =
        ReviewResponse.builder()
            .id(savedReview.getId())
            .contractId(contractId)
            .reviewerId(employerId)
            .reviewerName("Employer Corp")
            .revieweeId(employeeId)
            .revieweeName("Freelancer Pro")
            .rating(5)
            .comment("Excellent work")
            .build();
    when(reviewMapper.toResponse(any(Review.class))).thenReturn(mockResponse);

    ReviewResponse response = reviewService.createReview(request, userContext);

    assertThat(response).isNotNull();
    assertThat(response.getRating()).isEqualTo(5);
    verify(reviewRepository).save(any(Review.class));
    verify(applicationEventPublisher).publishEvent(any(ReviewCreatedDomainEvent.class));
  }

  @Test
  @DisplayName("Should forbid outsider from querying reviews by contract (IDOR Protection)")
  void shouldForbidOutsiderFromQueryingContractReviews() {
    UserContext strangerContext = new UserContext(strangerId, UserRole.USER, "stranger@test.local");
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    assertThatThrownBy(() -> reviewService.getReviewsByContract(contractId, strangerContext))
        .isInstanceOf(ReviewException.class)
        .matches(
            ex -> ((ReviewException) ex).getCode().equals(ReviewErrorCode.FORBIDDEN_REVIEW.code()));
  }

  @Test
  @DisplayName("Should allow contract participant (employer) to query contract reviews")
  void shouldAllowContractParticipantToQueryContractReviews() {
    UserContext employerContext = new UserContext(employerId, UserRole.USER, "employer@test.local");
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(reviewRepository.findByContractId(contractId)).thenReturn(List.of());

    List<ReviewResponse> reviews = reviewService.getReviewsByContract(contractId, employerContext);

    assertThat(reviews).isEmpty();
    verify(reviewRepository).findByContractId(contractId);
  }

  @Test
  @DisplayName("Should allow admin to query contract reviews")
  void shouldAllowAdminToQueryContractReviews() {
    UserContext adminContext = new UserContext(strangerId, UserRole.ADMIN, "admin@test.local");
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(reviewRepository.findByContractId(contractId)).thenReturn(List.of());

    List<ReviewResponse> reviews = reviewService.getReviewsByContract(contractId, adminContext);

    assertThat(reviews).isEmpty();
    verify(reviewRepository).findByContractId(contractId);
  }
}
