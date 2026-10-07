package com.occupify.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.SalaryType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BidTest {

  @Test
  void testBidBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    Job job = Job.builder().title("Dev").build();
    User freelancer = User.builder().email("free@test.com").build();

    Bid bid =
        Bid.builder()
            .job(job)
            .freelancer(freelancer)
            .proposedSalaryType(SalaryType.WEEKLY)
            .proposedRate(BigDecimal.valueOf(500.00))
            .commitmentDuration("3 months")
            .weeklyCommitmentHours("20 hours")
            .message("Ready to start")
            .attachmentCvUrl("https://example.com/cv.pdf")
            .portfolioLink("https://portfolio.com")
            .status(BidStatus.PENDING)
            .build();
    bid.setId(id);

    assertEquals(id, bid.getId());
    assertEquals(job, bid.getJob());
    assertEquals(freelancer, bid.getFreelancer());
    assertEquals(SalaryType.WEEKLY, bid.getProposedSalaryType());
    assertEquals(BigDecimal.valueOf(500.00), bid.getProposedRate());
    assertEquals("3 months", bid.getCommitmentDuration());
    assertEquals("20 hours", bid.getWeeklyCommitmentHours());
    assertEquals("Ready to start", bid.getMessage());
    assertEquals("https://example.com/cv.pdf", bid.getAttachmentCvUrl());
    assertEquals("https://portfolio.com", bid.getPortfolioLink());
    assertEquals(BidStatus.PENDING, bid.getStatus());
  }
}
