package com.occupify.core.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.entity.contract.Contract;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.contract.ContractRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContractSagaWatchdogTest {

  @Mock private ContractRepository contractRepository;
  @Mock private ContractEventProducer contractEventProducer;

  @InjectMocks private ContractSagaWatchdog watchdog;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(watchdog, "timeoutSeconds", 30);
  }

  @Test
  @DisplayName("Watchdog - Trips compensation when contracts are timed out in PENDING_ESCROW")
  void sweepTimedOutSagas_RevertsAndCompensates() {
    UUID contractId = UUID.randomUUID();
    Contract contract = new Contract();
    contract.setId(contractId);
    contract.setStatus(ContractStatus.PENDING_ESCROW);
    contract.setStartDate(LocalDate.now());

    when(contractRepository.findByStatusAndUpdatedAtBefore(
            eq(ContractStatus.PENDING_ESCROW), any(LocalDateTime.class)))
        .thenReturn(List.of(contract));

    watchdog.sweepTimedOutSagas();

    assertEquals(ContractStatus.PENDING, contract.getStatus());
    assertNull(contract.getStartDate());
    verify(contractRepository).save(contract);

    ArgumentCaptor<ContractCompensationRequestedEvent> captor =
        ArgumentCaptor.forClass(ContractCompensationRequestedEvent.class);
    verify(contractEventProducer).publishContractCompensation(captor.capture());

    ContractCompensationRequestedEvent event = captor.getValue();
    assertEquals(contractId, event.contractId());
    assertEquals("occupify-core-watchdog", event.failedService());
    assertEquals("PAYMENT_SERVICE_TIMEOUT", event.failureType());
  }

  @Test
  @DisplayName("Watchdog - Does nothing when no contracts are timed out")
  void sweepTimedOutSagas_NoTimedOutContracts() {
    when(contractRepository.findByStatusAndUpdatedAtBefore(
            eq(ContractStatus.PENDING_ESCROW), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    watchdog.sweepTimedOutSagas();

    verify(contractRepository, never()).save(any());
    verify(contractEventProducer, never()).publishContractCompensation(any());
  }
}
