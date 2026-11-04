package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.request.review.CreateReviewRequest;
import com.occupify.core.dto.response.review.ReviewResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(RouteConstants.REVIEWS)
@Tag(name = "Reviews", description = "Endpoints for creating and retrieving ratings and reviews")
public class ReviewController extends AbstractBaseController {

  private final ReviewService reviewService;

  @PostMapping
  @Operation(
      summary = "Submit a review",
      description = "Submits feedback and 1-5 star rating upon contract completion")
  public ResponseEntity<CreatedResponse<ReviewResponse>> createReview(
      @Valid @RequestBody CreateReviewRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    ReviewResponse response = reviewService.createReview(request, userContext);
    return created(response, MessageConstants.REVIEW_CREATED);
  }

  @GetMapping(RouteConstants.REVIEW_CONTRACT)
  @Operation(
      summary = "Get reviews for a contract",
      description = "Retrieves reviews associated with a specific completed contract")
  public ResponseEntity<SingleResponse<List<ReviewResponse>>> getReviewsByContract(
      @PathVariable UUID contractId,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    List<ReviewResponse> response = reviewService.getReviewsByContract(contractId, userContext);
    return successSingle(response, MessageConstants.REVIEW_LIST_RETRIEVED);
  }

  @GetMapping(RouteConstants.REVIEW_USER)
  @Operation(
      summary = "Get reviews for a user",
      description = "Retrieves paginated reviews received by a user")
  public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByUser(
      @PathVariable UUID userId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<ReviewResponse> page = reviewService.getReviewsByUser(userId, pageable);
    return paging(page, MessageConstants.REVIEW_LIST_RETRIEVED);
  }
}
