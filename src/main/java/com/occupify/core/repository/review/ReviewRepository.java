package com.occupify.core.repository.review;

import com.occupify.core.entity.review.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
  List<Review> findByContractId(UUID contractId);

  List<Review> findByReviewerId(UUID reviewerId);

  List<Review> findByRevieweeId(UUID revieweeId);

  boolean existsByContractIdAndReviewerId(UUID contractId, UUID reviewerId);

  Page<Review> findByRevieweeId(UUID revieweeId, Pageable pageable);

  Page<Review> findByContractId(UUID contractId, Pageable pageable);
}
