package com.occupify.core.repository.contract;

import com.occupify.core.dto.request.contract.ContractFilterRequest;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.security.UserContext;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ContractSpecification {

  private ContractSpecification() {}

  public static Specification<Contract> withFilter(
      ContractFilterRequest filter, UserContext userContext) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (userContext != null && !userContext.isAdmin()) {
        UUID currentUserId = userContext.userId();
        Predicate isEmployer = criteriaBuilder.equal(root.get("employer").get("id"), currentUserId);
        Predicate isEmployee = criteriaBuilder.equal(root.get("employee").get("id"), currentUserId);
        predicates.add(criteriaBuilder.or(isEmployer, isEmployee));
      }

      if (filter != null) {
        if (filter.projectId() != null) {
          predicates.add(
              criteriaBuilder.equal(root.get("job").get("project").get("id"), filter.projectId()));
        }
        if (filter.jobId() != null) {
          predicates.add(criteriaBuilder.equal(root.get("job").get("id"), filter.jobId()));
        }
        if (filter.status() != null) {
          predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
        }
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
