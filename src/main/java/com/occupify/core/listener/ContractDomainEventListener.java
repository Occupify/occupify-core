package com.occupify.core.listener;

import com.occupify.core.event.domain.ContractCancelledDomainEvent;
import com.occupify.core.event.domain.ContractCompletedDomainEvent;
import com.occupify.core.event.domain.ReviewCreatedDomainEvent;
import com.occupify.core.producer.ContractEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractDomainEventListener {

  private final ContractEventProducer contractEventProducer;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onContractCompleted(ContractCompletedDomainEvent domainEvent) {
    log.info(
        "[Domain Event Listener] Transaction committed. Emitting ContractCompletedEvent for contract [{}]",
        domainEvent.payload().contractId());
    contractEventProducer.publishContractCompleted(domainEvent.payload());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onContractCancelled(ContractCancelledDomainEvent domainEvent) {
    log.info(
        "[Domain Event Listener] Transaction committed. Emitting ContractCancelledEvent for contract [{}]",
        domainEvent.payload().contractId());
    contractEventProducer.publishContractCancelled(domainEvent.payload());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onReviewCreated(ReviewCreatedDomainEvent domainEvent) {
    log.info(
        "[Domain Event Listener] Transaction committed. Emitting ReviewCreatedEvent for review [{}]",
        domainEvent.payload().reviewId());
    contractEventProducer.publishReviewCreated(domainEvent.payload());
  }
}
