package com.occupify.core.service;

import com.occupify.core.dto.request.BidFilterRequest;
import com.occupify.core.dto.request.CreateBidRequest;
import com.occupify.core.dto.request.UpdateBidRequest;
import com.occupify.core.dto.response.BidResponse;
import com.occupify.core.security.UserContext;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BidService {

  BidResponse createBid(CreateBidRequest request, UserContext userContext);

  Page<BidResponse> getMyBids(BidFilterRequest filter, Pageable pageable, UserContext userContext);

  Page<BidResponse> getBids(BidFilterRequest filter, Pageable pageable, UserContext userContext);

  BidResponse getBidDetail(UUID id, UserContext userContext);

  BidResponse getMyBidForJob(UUID jobId, UserContext userContext);

  BidResponse updateBid(UUID id, UpdateBidRequest request, UserContext userContext);

  void cancelBid(UUID id, UserContext userContext);
}
