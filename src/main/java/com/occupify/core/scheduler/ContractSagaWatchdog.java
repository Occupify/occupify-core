package com.occupify.core.scheduler;

import com.occupify.core.entity.contract.Contract;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.producer.ContractEventProducer;
import com.occupify.core.repository.contract.ContractRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractSagaWatchdog {

  private final ContractRepository contractRepository;
  private final ContractEventProducer contractEventProducer;

  @Value("${app.saga.watchdog-timeout-seconds:30}")
  private int timeoutSeconds;

  @Scheduled(fixedDelayString = "${app.saga.watchdog-interval-ms:10000}")
  @Transactional
  public void sweepTimedOutSagas() {
    LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(timeoutSeconds);
    List<Contract> timedOutContracts =
        contractRepository.findByStatusAndUpdatedAtBefore(
            ContractStatus.PENDING_ESCROW, timeoutThreshold);

    if (timedOutContracts.isEmpty()) {
      return;
    }

    for (Contract contract : timedOutContracts) {
      log.error(
          "Contract [{}] timed out in PENDING_ESCROW (> {}s). Reverting to PENDING and emitting compensation",
          contract.getId(),
          timeoutSeconds);

      contract.setStatus(ContractStatus.PENDING);
      contract.setStartedAt(null);
      contractRepository.save(contract);

      ContractCompensationRequestedEvent compensationEvent =
          new ContractCompensationRequestedEvent(
              UUID.randomUUID(),
              contract.getId(),
              "occupify-core-watchdog",
              "PAYMENT_SERVICE_TIMEOUT",
              "Payment service failed to lock escrow within " + timeoutSeconds + "s timeout",
              LocalDateTime.now());

      contractEventProducer.publishContractCompensation(compensationEvent);
    }
  }
}
