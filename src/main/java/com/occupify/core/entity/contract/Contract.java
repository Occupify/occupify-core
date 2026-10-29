package com.occupify.core.entity.contract;

import com.occupify.core.entity.base.AbstractBaseEntity;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contracts")
public class Contract extends AbstractBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id", nullable = false)
  private User employer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id")
  private @Nullable User employee;

  @Column(name = "role", length = 100)
  private @Nullable String role;

  @Column(name = "title", length = 255)
  private @Nullable String title;

  @Column(name = "freelancer_email")
  private @Nullable String freelancerEmail;

  @Column(name = "message", columnDefinition = "TEXT")
  private @Nullable String message;

  @Column(name = "terms_file_key", length = 500)
  private @Nullable String termsFileKey;

  @Column(name = "job_description", columnDefinition = "TEXT")
  private @Nullable String jobDescription;

  @Column(name = "salary_due_date", length = 100)
  private @Nullable String salaryDueDate;

  @Column(name = "source_type", length = 50)
  private @Nullable String sourceType;

  @Column(name = "source_id")
  private @Nullable UUID sourceId;

  @Column(name = "agreed_rate", precision = 12, scale = 2)
  private @Nullable BigDecimal agreedRate;

  @Column(name = "salary_type", length = 50)
  private @Nullable String salaryType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ContractStatus status;

  @Column(name = "payment_status", length = 50)
  private @Nullable String paymentStatus;

  @Column(name = "started_at")
  private @Nullable LocalDateTime startedAt;

  @Column(name = "finished_at")
  private @Nullable LocalDateTime finishedAt;

  @Column(name = "deleted_at")
  private @Nullable LocalDateTime deletedAt;

  public String getEffectiveTitle() {
    return title != null && !title.isBlank() ? title : (job != null ? job.getTitle() : null);
  }

  public String getEffectiveRole() {
    return role != null && !role.isBlank() ? role : (job != null ? job.getRole() : null);
  }

  public String getEffectiveDescription() {
    return jobDescription != null && !jobDescription.isBlank()
        ? jobDescription
        : (job != null ? job.getDescription() : null);
  }

  public BigDecimal getEffectiveRate() {
    if (agreedRate != null) {
      return agreedRate;
    }
    if (job != null) {
      return job.getBudget() != null ? job.getBudget() : job.getBudgetMin();
    }
    return null;
  }

  public String getEffectiveSalaryType() {
    return salaryType != null && !salaryType.isBlank()
        ? salaryType
        : (job != null && job.getSalaryType() != null ? job.getSalaryType().name() : null);
  }

  public String getEffectiveTermsFileKey() {
    return termsFileKey != null && !termsFileKey.isBlank()
        ? termsFileKey
        : (job != null ? job.getTermsFileKey() : null);
  }

  public LocalDateTime getEffectiveStartedAt() {
    return startedAt != null ? startedAt : (job != null ? job.getStartedAt() : null);
  }

  public LocalDateTime getEffectiveFinishedAt() {
    return finishedAt != null ? finishedAt : (job != null ? job.getFinishedAt() : null);
  }
}
