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

  @Column(name = "source_type", length = 50)
  private @Nullable String sourceType;

  @Column(name = "source_id")
  private @Nullable UUID sourceId;

  @Column(name = "agreed_rate", precision = 12, scale = 2, nullable = false)
  private BigDecimal agreedRate;

  @Column(name = "salary_type", nullable = false, length = 50)
  private String salaryType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ContractStatus status;

  @Column(name = "payment_status", length = 50)
  private @Nullable String paymentStatus;

  @Column(name = "started_at")
  private @Nullable LocalDateTime startedAt;

  @Column(name = "finished_at")
  private @Nullable LocalDateTime finishedAt;
}
