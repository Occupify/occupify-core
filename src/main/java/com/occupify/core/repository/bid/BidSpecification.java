package com.occupify.core.repository.bid;

import com.occupify.core.dto.request.BidFilterRequest;
import com.occupify.core.entity.bid.Bid;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BidSpecification {

  private BidSpecification() {}

  public static Specification<Bid> withFilters(BidFilterRequest filter, UUID defaultFreelancerId) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      UUID effectiveFreelancerId = defaultFreelancerId;
      if (filter != null && Boolean.FALSE.equals(filter.mine())) {
        // Explicitly not filtered to current user (e.g. employer viewing project/job bids)
        effectiveFreelancerId = null;
      }

      if (effectiveFreelancerId != null) {
        predicates.add(
            criteriaBuilder.equal(root.get("freelancer").get("id"), effectiveFreelancerId));
      }

      if (filter != null) {
        if (filter.jobId() != null) {
          predicates.add(criteriaBuilder.equal(root.get("job").get("id"), filter.jobId()));
        }

        if (filter.projectId() != null) {
          predicates.add(
              criteriaBuilder.equal(root.get("job").get("project").get("id"), filter.projectId()));
        }

        if (filter.status() != null) {
          predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
        }
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
