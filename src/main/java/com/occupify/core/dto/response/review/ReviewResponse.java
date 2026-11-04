package com.occupify.core.dto.response.review;

import java.time.LocalDateTime;
import java.util.UUID;
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
public class ReviewResponse {
  private UUID id;
  private UUID contractId;
  private UUID reviewerId;
  private String reviewerName;
  private UUID revieweeId;
  private String revieweeName;
  private Integer rating;
  private String comment;
  private LocalDateTime createdAt;
}
