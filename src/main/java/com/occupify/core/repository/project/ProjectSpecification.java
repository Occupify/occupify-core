package com.occupify.core.repository.project;

import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProjectSpecification {

  private ProjectSpecification() {}

  public static Specification<Project> withFilters(ProjectFilterRequest filter) {
    return (root, query, criteriaBuilder) -> {
      if (query != null) {
        query.distinct(true);
      }

      List<Predicate> predicates = new ArrayList<>();

      // 1. Status and Recruiting Jobs filter
      if (filter != null && filter.status() != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
      } else if (filter == null || filter.employerId() == null) {
        // Default public search: project must be OPEN and have at least 1 RECRUITING job
        predicates.add(criteriaBuilder.equal(root.get("status"), ProjectStatus.OPEN));

        Subquery<Long> recruitingJobSubquery = query.subquery(Long.class);
        Root<Job> jobRoot = recruitingJobSubquery.from(Job.class);
        recruitingJobSubquery
            .select(criteriaBuilder.literal(1L))
            .where(
                criteriaBuilder.equal(jobRoot.get("project"), root),
                criteriaBuilder.equal(jobRoot.get("status"), JobStatus.RECRUITING));
        predicates.add(criteriaBuilder.exists(recruitingJobSubquery));
      }

      if (filter == null) {
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
      }

      // 2. Keyword Full-Text Search (unaccented) across project and recruiting jobs/skills
      if (StringUtils.hasText(filter.keyword())) {
        String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
        Expression<String> unaccentPattern =
            criteriaBuilder.function("unaccent", String.class, criteriaBuilder.literal(pattern));

        // Match project title
        Expression<String> unaccentTitle =
            criteriaBuilder.function(
                "unaccent", String.class, criteriaBuilder.lower(root.get("title")));
        Predicate titlePredicate = criteriaBuilder.like(unaccentTitle, unaccentPattern);

        // Match project description
        Expression<String> unaccentDesc =
            criteriaBuilder.function(
                "unaccent",
                String.class,
                criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")));
        Predicate descPredicate = criteriaBuilder.like(unaccentDesc, unaccentPattern);

        // Match recruiting job title, description, or skill name
        Subquery<Long> kwJobSubquery = query.subquery(Long.class);
        Root<Job> kwJobRoot = kwJobSubquery.from(Job.class);
        SetJoin<Job, Skill> skillJoin = kwJobRoot.joinSet("skills", JoinType.LEFT);

        Expression<String> unaccentJobTitle =
            criteriaBuilder.function(
                "unaccent", String.class, criteriaBuilder.lower(kwJobRoot.get("title")));
        Expression<String> unaccentJobDesc =
            criteriaBuilder.function(
                "unaccent",
                String.class,
                criteriaBuilder.lower(criteriaBuilder.coalesce(kwJobRoot.get("description"), "")));
        Expression<String> unaccentSkillName =
            criteriaBuilder.function(
                "unaccent",
                String.class,
                criteriaBuilder.lower(criteriaBuilder.coalesce(skillJoin.get("name"), "")));

        Predicate kwMatch =
            criteriaBuilder.or(
                criteriaBuilder.like(unaccentJobTitle, unaccentPattern),
                criteriaBuilder.like(unaccentJobDesc, unaccentPattern),
                criteriaBuilder.like(unaccentSkillName, unaccentPattern));

        kwJobSubquery
            .select(criteriaBuilder.literal(1L))
            .where(
                criteriaBuilder.equal(kwJobRoot.get("project"), root),
                criteriaBuilder.equal(kwJobRoot.get("status"), JobStatus.RECRUITING),
                kwMatch);

        predicates.add(
            criteriaBuilder.or(
                titlePredicate, descPredicate, criteriaBuilder.exists(kwJobSubquery)));
      }

      // 3. Skills Filter (OR logic on recruiting jobs)
      if (filter.skills() != null && !filter.skills().isEmpty()) {
        Subquery<Long> skillSubquery = query.subquery(Long.class);
        Root<Job> skillJobRoot = skillSubquery.from(Job.class);
        SetJoin<Job, Skill> skillJoin = skillJobRoot.joinSet("skills");

        List<Predicate> skillPredicates = new ArrayList<>();
        for (String skillName : filter.skills()) {
          if (StringUtils.hasText(skillName)) {
            Expression<String> unaccentTargetSkill =
                criteriaBuilder.function(
                    "unaccent",
                    String.class,
                    criteriaBuilder.literal(skillName.trim().toLowerCase()));
            Expression<String> unaccentSkillName =
                criteriaBuilder.function(
                    "unaccent", String.class, criteriaBuilder.lower(skillJoin.get("name")));
            skillPredicates.add(criteriaBuilder.equal(unaccentSkillName, unaccentTargetSkill));
          }
        }

        if (!skillPredicates.isEmpty()) {
          skillSubquery
              .select(criteriaBuilder.literal(1L))
              .where(
                  criteriaBuilder.equal(skillJobRoot.get("project"), root),
                  criteriaBuilder.equal(skillJobRoot.get("status"), JobStatus.RECRUITING),
                  criteriaBuilder.or(skillPredicates.toArray(new Predicate[0])));
          predicates.add(criteriaBuilder.exists(skillSubquery));
        }
      }

      // 4. Budget Overlap Filter on recruiting jobs
      if (filter.minPrice() != null || filter.maxPrice() != null) {
        Subquery<Long> budgetSubquery = query.subquery(Long.class);
        Root<Job> budgetJobRoot = budgetSubquery.from(Job.class);

        List<Predicate> budgetPredicates = new ArrayList<>();
        budgetPredicates.add(criteriaBuilder.equal(budgetJobRoot.get("project"), root));
        budgetPredicates.add(
            criteriaBuilder.equal(budgetJobRoot.get("status"), JobStatus.RECRUITING));

        if (filter.minPrice() != null) {
          Predicate maxGteMin =
              criteriaBuilder.greaterThanOrEqualTo(
                  budgetJobRoot.get("budgetMax"), filter.minPrice());
          Predicate minGteMin =
              criteriaBuilder.greaterThanOrEqualTo(
                  budgetJobRoot.get("budgetMin"), filter.minPrice());
          budgetPredicates.add(criteriaBuilder.or(maxGteMin, minGteMin));
        }

        if (filter.maxPrice() != null) {
          Predicate minLteMax =
              criteriaBuilder.lessThanOrEqualTo(budgetJobRoot.get("budgetMin"), filter.maxPrice());
          Predicate maxLteMax =
              criteriaBuilder.lessThanOrEqualTo(budgetJobRoot.get("budgetMax"), filter.maxPrice());
          budgetPredicates.add(criteriaBuilder.or(minLteMax, maxLteMax));
        }

        budgetSubquery
            .select(criteriaBuilder.literal(1L))
            .where(budgetPredicates.toArray(new Predicate[0]));
        predicates.add(criteriaBuilder.exists(budgetSubquery));
      }

      // 5. EmployerId filter (for employer management views)
      if (filter.employerId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("employer").get("id"), filter.employerId()));
      }

      // 5b. Exclude projects belonging to the searching user
      if (filter.excludeEmployerId() != null) {
        predicates.add(
            criteriaBuilder.notEqual(root.get("employer").get("id"), filter.excludeEmployerId()));
      }

      // 6. Period and ExperienceLevel filters
      if (filter.period() != null) {
        predicates.add(criteriaBuilder.equal(root.get("period"), filter.period()));
      }

      if (filter.experienceLevel() != null) {
        predicates.add(
            criteriaBuilder.equal(root.get("experienceLevel"), filter.experienceLevel()));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
