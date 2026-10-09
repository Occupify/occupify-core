package com.occupify.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewTest {

  @Test
  void testReviewBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    Contract contract = Contract.builder().title("Contract").build();
    User reviewer = User.builder().email("client@test.com").build();
    User reviewee = User.builder().email("freelancer@test.com").build();

    Review review =
        Review.builder()
            .contract(contract)
            .reviewer(reviewer)
            .reviewee(reviewee)
            .rating(5)
            .comment("Great cooperation")
            .build();
    review.setId(id);

    assertEquals(id, review.getId());
    assertEquals(contract, review.getContract());
    assertEquals(reviewer, review.getReviewer());
    assertEquals(reviewee, review.getReviewee());
    assertEquals(5, review.getRating());
    assertEquals("Great cooperation", review.getComment());
  }
}
