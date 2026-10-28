package com.occupify.core.entity.job;

import com.occupify.core.entity.base.AbstractBaseEntity;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.SalaryType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

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

  @Column(name = "role", length = 100)
  private String role;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "budget", precision = 12, scale = 2)
  private BigDecimal budget;

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

  @Column(name = "terms_file_key", length = 500)
  private String termsFileKey;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private JobStatus status;

  @Builder.Default
  @BatchSize(size = 50)
  @ManyToMany
  @JoinTable(
      name = "job_skills",
      joinColumns = @JoinColumn(name = "job_id"),
      inverseJoinColumns = @JoinColumn(name = "skill_id"))
  private Set<Skill> skills = new HashSet<>();

  @Builder.Default
  @ElementCollection
  @CollectionTable(name = "job_requirement", joinColumns = @JoinColumn(name = "job_id"))
  @OrderColumn(name = "position")
  @Column(name = "text", length = 300)
  @BatchSize(size = 50)
  private List<String> requirements = new ArrayList<>();

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
