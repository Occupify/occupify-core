package com.occupify.core.entity;

import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.SalaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "bids")
public class Bid extends AbstractBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "freelancer_id", nullable = false)
  private User freelancer;

  @Enumerated(EnumType.STRING)
  @Column(name = "proposed_salary_type", length = 50)
  private SalaryType proposedSalaryType;

  @Column(name = "proposed_rate", precision = 12, scale = 2)
  private BigDecimal proposedRate;

  @Column(name = "commitment_duration", length = 100)
  private String commitmentDuration;

  @Column(name = "weekly_commitment_hours", length = 50)
  private String weeklyCommitmentHours;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "attachment_cv_url")
  private String attachmentCvUrl;

  @Column(name = "portfolio_link")
  private String portfolioLink;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private BidStatus status;
}
