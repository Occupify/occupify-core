package com.occupify.core.repository;

import com.occupify.core.entity.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
  List<Review> findByContractId(UUID contractId);

  List<Review> findByReviewerId(UUID reviewerId);

  List<Review> findByRevieweeId(UUID revieweeId);
}
