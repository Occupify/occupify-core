package com.occupify.core.repository;

import com.occupify.core.entity.Bid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, UUID> {
  List<Bid> findByJobId(UUID jobId);

  List<Bid> findByFreelancerId(UUID freelancerId);
}
