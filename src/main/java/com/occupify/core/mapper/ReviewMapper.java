package com.occupify.core.mapper;

import com.occupify.core.dto.response.review.ReviewResponse;
import com.occupify.core.entity.review.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

  public ReviewResponse toResponse(Review review) {
    if (review == null) {
      return null;
    }

    String reviewerName = null;
    if (review.getReviewer() != null) {
      reviewerName =
          review.getReviewer().getFullName() != null
                  && !review.getReviewer().getFullName().isBlank()
              ? review.getReviewer().getFullName()
              : review.getReviewer().getEmail();
    }

    String revieweeName = null;
    if (review.getReviewee() != null) {
      revieweeName =
          review.getReviewee().getFullName() != null
                  && !review.getReviewee().getFullName().isBlank()
              ? review.getReviewee().getFullName()
              : review.getReviewee().getEmail();
    }

    return ReviewResponse.builder()
        .id(review.getId())
        .contractId(review.getContract() != null ? review.getContract().getId() : null)
        .reviewerId(review.getReviewer() != null ? review.getReviewer().getId() : null)
        .reviewerName(reviewerName)
        .revieweeId(review.getReviewee() != null ? review.getReviewee().getId() : null)
        .revieweeName(revieweeName)
        .rating(review.getRating())
        .comment(review.getComment())
        .createdAt(review.getCreatedAt())
        .build();
  }
}
