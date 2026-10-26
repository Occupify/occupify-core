package com.occupify.core.listener;

import static org.mockito.Mockito.verify;

import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.event.ContractEscrowLockedEvent;
import com.occupify.core.service.ContractService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractEventListenerTest {

  @Mock private ContractService contractService;

  @InjectMocks private ContractEventListener contractEventListener;

  @Test
  @DisplayName("Should invoke compensateContract when ContractCompensationRequestedEvent arrives")
  void handleCompensationRequested_CallsService() {
    UUID contractId = UUID.randomUUID();
    ContractCompensationRequestedEvent event =
        new ContractCompensationRequestedEvent(
            UUID.randomUUID(),
            contractId,
            "occupify-payment",
            "INSUFFICIENT_FUNDS",
            "Balance low",
            LocalDateTime.now());

    contractEventListener.handleCompensationRequested(event);

    verify(contractService).compensateContract(contractId, "Balance low");
  }

  @Test
  @DisplayName("Should invoke confirmContractEscrow when ContractEscrowLockedEvent arrives")
  void handleEscrowLocked_CallsService() {
    UUID contractId = UUID.randomUUID();
    ContractEscrowLockedEvent event =
        new ContractEscrowLockedEvent(UUID.randomUUID(), contractId, LocalDateTime.now());

    contractEventListener.handleEscrowLocked(event);

    verify(contractService).confirmContractEscrow(contractId);
  }
}
