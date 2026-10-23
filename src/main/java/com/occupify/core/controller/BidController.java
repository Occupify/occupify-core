package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.base.SingleResponse;
import com.occupify.core.dto.base.SuccessResponse;
import com.occupify.core.dto.request.BidFilterRequest;
import com.occupify.core.dto.request.CreateBidRequest;
import com.occupify.core.dto.request.UpdateBidRequest;
import com.occupify.core.dto.response.BidResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(RouteConstants.BIDS)
@Tag(
    name = "Bids",
    description =
        "Endpoints for submitting, querying, updating, and cancelling project job proposals")
public class BidController extends AbstractBaseController {

  private final BidService bidService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Submit a bid for a job role",
      description =
          "Submits a proposal with optional CV document (PDF/Image) for a recruiting job in an open project. Authorized for users (employees).")
  public ResponseEntity<CreatedResponse<BidResponse>> createBid(
      @Valid @ModelAttribute CreateBidRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    BidResponse response = bidService.createBid(request, userContext);
    return created(response, MessageConstants.BID_CREATED);
  }

  @GetMapping(RouteConstants.BIDS_MY)
  @Operation(
      summary = "Get employee's submitted bids",
      description = "Retrieves a paginated list of proposals submitted by the authenticated user.")
  public ResponseEntity<PageResponse<BidResponse>> getMyBids(
      @ParameterObject BidFilterRequest filter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    Page<BidResponse> page = bidService.getMyBids(filter, pageable, userContext);
    return paging(page, MessageConstants.BID_LIST_RETRIEVED);
  }

  @GetMapping
  @Operation(
      summary = "Get paginated bids with filters",
      description =
          "Retrieves a paginated list of proposals filtered by job, project, or status. Permitted for applicants or project owners.")
  public ResponseEntity<PageResponse<BidResponse>> getBids(
      @ParameterObject BidFilterRequest filter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    Page<BidResponse> page = bidService.getBids(filter, pageable, userContext);
    return paging(page, MessageConstants.BID_LIST_RETRIEVED);
  }

  @GetMapping(RouteConstants.BID_JOB)
  @Operation(
      summary = "Get employee's bid on a specific job",
      description =
          "Retrieves the authenticated employee's existing proposal for the specified job role.")
  public ResponseEntity<SingleResponse<BidResponse>> getMyBidForJob(
      @PathVariable UUID jobId, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    BidResponse response = bidService.getMyBidForJob(jobId, userContext);
    return successSingle(response, MessageConstants.BID_RETRIEVED);
  }

  @GetMapping(RouteConstants.BID_ID)
  @Operation(
      summary = "Get bid proposal details",
      description =
          "Retrieves full details of a specific bid proposal. Accessible by applicant or project owner.")
  public ResponseEntity<SingleResponse<BidResponse>> getBidDetail(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    BidResponse response = bidService.getBidDetail(id, userContext);
    return successSingle(response, MessageConstants.BID_RETRIEVED);
  }

  @PutMapping(value = RouteConstants.BID_ID, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Update an existing bid proposal",
      description =
          "Updates rate, message, or attachments of a pending proposal. Only permitted by applicant while status is PENDING.")
  public ResponseEntity<SingleResponse<BidResponse>> updateBid(
      @PathVariable UUID id,
      @Valid @ModelAttribute UpdateBidRequest request,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    BidResponse response = bidService.updateBid(id, request, userContext);
    return successSingle(response, MessageConstants.BID_UPDATED);
  }

  @DeleteMapping(RouteConstants.BID_ID)
  @Operation(
      summary = "Withdraw / cancel a pending bid proposal",
      description =
          "Withdraws and deletes a proposal. Only permitted by applicant while status is PENDING.")
  public ResponseEntity<SuccessResponse> cancelBid(
      @PathVariable UUID id, @Parameter(hidden = true) @Authenticated UserContext userContext) {
    bidService.cancelBid(id, userContext);
    return success(MessageConstants.BID_CANCELLED);
  }
}
