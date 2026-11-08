package com.occupify.core.service.impl;

import com.occupify.core.dto.request.review.CreateReviewRequest;
import com.occupify.core.dto.response.review.ReviewResponse;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.review.Review;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.event.ReviewCreatedEvent;
import com.occupify.core.event.domain.ReviewCreatedDomainEvent;
import com.occupify.core.exception.contract.ContractErrorCode;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.exception.review.ReviewErrorCode;
import com.occupify.core.exception.review.ReviewException;
import com.occupify.core.mapper.ReviewMapper;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.review.ReviewRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ReviewService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final ContractRepository contractRepository;
  private final UserRepository userRepository;
  private final ReviewMapper reviewMapper;
  private final ContractEventProducer contractEventProducer;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  public ReviewResponse createReview(CreateReviewRequest request, UserContext userContext) {
    log.info(
        "[ReviewService] Submitting review for contract [{}], reviewer [{}]",
        request.contractId(),
        userContext.userId());

    Contract contract =
        contractRepository
            .findById(request.contractId())
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

    if (contract.getStatus() != ContractStatus.COMPLETED) {
      throw new ReviewException(ReviewErrorCode.CONTRACT_NOT_COMPLETED);
    }

    boolean isEmployer =
        contract.getEmployer() != null
            && contract.getEmployer().getId().equals(userContext.userId());
    boolean isEmployee =
        (contract.getEmployee() != null
                && contract.getEmployee().getId().equals(userContext.userId()))
            || (contract.getFreelancerEmail() != null
                && contract.getFreelancerEmail().equalsIgnoreCase(userContext.email()));

    if (!isEmployer && !isEmployee) {
      throw new ReviewException(ReviewErrorCode.FORBIDDEN_REVIEW);
    }

    User reviewer =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(() -> new ContractException(ContractErrorCode.USER_NOT_FOUND));

    User reviewee = isEmployer ? contract.getEmployee() : contract.getEmployer();
    if (reviewee == null || reviewer.getId().equals(reviewee.getId())) {
      throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_TARGET);
    }

    if (reviewRepository.existsByContractIdAndReviewerId(contract.getId(), reviewer.getId())) {
      throw new ReviewException(ReviewErrorCode.DUPLICATE_REVIEW);
    }

    Review review =
        Review.builder()
            .contract(contract)
            .reviewer(reviewer)
            .reviewee(reviewee)
            .rating(request.rating())
            .comment(request.comment())
            .build();

    review = reviewRepository.save(review);

    String reviewerName =
        reviewer.getFullName() != null && !reviewer.getFullName().isBlank()
            ? reviewer.getFullName()
            : reviewer.getEmail();
    String revieweeName =
        reviewee.getFullName() != null && !reviewee.getFullName().isBlank()
            ? reviewee.getFullName()
            : reviewee.getEmail();

    ReviewCreatedEvent event =
        new ReviewCreatedEvent(
            UUID.randomUUID(),
            review.getId(),
            contract.getId(),
            reviewer.getId(),
            reviewerName,
            reviewee.getId(),
            revieweeName,
            reviewee.getEmail(),
            review.getRating(),
            review.getComment(),
            LocalDateTime.now());
    applicationEventPublisher.publishEvent(new ReviewCreatedDomainEvent(event));

    return reviewMapper.toResponse(review);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReviewResponse> getReviewsByContract(UUID contractId, UserContext userContext) {
    log.info("[ReviewService] Getting reviews for contract [{}]", contractId);

    Contract contract =
        contractRepository
            .findById(contractId)
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

    if (!userContext.isAdmin()) {
      boolean isEmployer =
          contract.getEmployer() != null
              && contract.getEmployer().getId().equals(userContext.userId());
      boolean isEmployee =
          (contract.getEmployee() != null
                  && contract.getEmployee().getId().equals(userContext.userId()))
              || (contract.getFreelancerEmail() != null
                  && contract.getFreelancerEmail().equalsIgnoreCase(userContext.email()));
      if (!isEmployer && !isEmployee) {
        throw new ReviewException(ReviewErrorCode.FORBIDDEN_REVIEW);
      }
    }

    return reviewRepository.findByContractId(contractId).stream()
        .map(reviewMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReviewResponse> getReviewsByUser(UUID userId, Pageable pageable) {
    log.info("[ReviewService] Getting reviews for user [{}]", userId);
    return reviewRepository.findByRevieweeId(userId, pageable).map(reviewMapper::toResponse);
  }
}
