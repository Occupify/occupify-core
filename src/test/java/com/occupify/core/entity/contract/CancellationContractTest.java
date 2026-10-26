package com.occupify.core.entity.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.entity.user.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CancellationContractTest {

  @Test
  void testCancellationContractBuilderAndGetters() {
    UUID id = UUID.randomUUID();
    Contract contract = Contract.builder().title("Contract").build();
    User requester = User.builder().email("requester@test.com").build();

    CancellationContract cancellation =
        CancellationContract.builder()
            .contract(contract)
            .requestedByUser(requester)
            .reason("Reason for cancel")
            .evidenceFileKey("evidence/file.png")
            .status("ACCEPTED")
            .build();
    cancellation.setId(id);

    assertEquals(id, cancellation.getId());
    assertEquals(contract, cancellation.getContract());
    assertEquals(requester, cancellation.getRequestedByUser());
    assertEquals("Reason for cancel", cancellation.getReason());
    assertEquals("evidence/file.png", cancellation.getEvidenceFileKey());
    assertEquals("ACCEPTED", cancellation.getStatus());
  }

  @Test
  void testDefaultStatus() {
    CancellationContract cancellation = CancellationContract.builder().build();
    assertEquals("ACCEPTED", cancellation.getStatus());
  }
}
