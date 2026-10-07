package com.occupify.core.entity;

import com.occupify.core.enums.OfferStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfferTest {

    @Test
    void testOfferBuilderAndGetters() {
        UUID id = UUID.randomUUID();
        Contract contract = Contract.builder().title("Contract").build();
        User freelancer = User.builder().email("free@test.com").build();

        Offer offer = Offer.builder()
                .contract(contract)
                .freelancer(freelancer)
                .proposedRate(BigDecimal.valueOf(1200.00))
                .offerMessage("Special offer")
                .status(OfferStatus.ACCEPTED)
                .build();
        offer.setId(id);

        assertEquals(id, offer.getId());
        assertEquals(contract, offer.getContract());
        assertEquals(freelancer, offer.getFreelancer());
        assertEquals(BigDecimal.valueOf(1200.00), offer.getProposedRate());
        assertEquals("Special offer", offer.getOfferMessage());
        assertEquals(OfferStatus.ACCEPTED, offer.getStatus());
    }
}
