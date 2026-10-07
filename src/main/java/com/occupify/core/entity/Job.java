package com.occupify.core.entity;

import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "job")
public class Job extends AbstractBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Builder.Default
  @Column(name = "quantity", nullable = false)
  private Integer quantity = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "salary_type", length = 50)
  private SalaryType salaryType;

  @Column(name = "budget_min", precision = 12, scale = 2)
  private BigDecimal budgetMin;

  @Column(name = "budget_max", precision = 12, scale = 2)
  private BigDecimal budgetMax;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private JobStatus status;

  @Builder.Default
  @ManyToMany
  @JoinTable(
      name = "job_skills",
      joinColumns = @JoinColumn(name = "job_id"),
      inverseJoinColumns = @JoinColumn(name = "skill_id"))
  private Set<Skill> skills = new HashSet<>();
}
