package com.occupify.core.repository.project;

import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.entity.project.Project;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProjectSpecification {

  private ProjectSpecification() {}

  public static Specification<Project> withFilters(ProjectFilterRequest filter) {
    return (root, query, criteriaBuilder) -> {
      if (filter == null) {
        return criteriaBuilder.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.hasText(filter.keyword())) {
        String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
        Predicate titlePredicate =
            criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
        Predicate descPredicate =
            criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
        predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
      }

      if (filter.status() != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
      }

      if (filter.employerId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("employer").get("id"), filter.employerId()));
      }

      if (filter.period() != null) {
        predicates.add(criteriaBuilder.equal(root.get("period"), filter.period()));
      }

      if (filter.experienceLevel() != null) {
        predicates.add(
            criteriaBuilder.equal(root.get("experienceLevel"), filter.experienceLevel()));
      }

      if (filter.minPrice() != null) {
        predicates.add(
            criteriaBuilder.greaterThanOrEqualTo(root.get("startPrice"), filter.minPrice()));
      }

      if (filter.maxPrice() != null) {
        predicates.add(
            criteriaBuilder.lessThanOrEqualTo(root.get("startPrice"), filter.maxPrice()));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
