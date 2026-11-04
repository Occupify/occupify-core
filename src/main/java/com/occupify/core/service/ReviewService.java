package com.occupify.core.service;

import com.occupify.core.dto.request.review.CreateReviewRequest;
import com.occupify.core.dto.response.review.ReviewResponse;
import com.occupify.core.security.UserContext;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

  ReviewResponse createReview(CreateReviewRequest request, UserContext userContext);

  List<ReviewResponse> getReviewsByContract(UUID contractId, UserContext userContext);

  Page<ReviewResponse> getReviewsByUser(UUID userId, Pageable pageable);
}
