package com.occupify.core.producer;

import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractCancelledEvent;
import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.event.ContractCompletedEvent;
import com.occupify.core.event.ContractCreatedEvent;
import com.occupify.core.event.ContractRejectedEvent;
import com.occupify.core.event.ReviewCreatedEvent;

public interface ContractEventProducer {
  void publishContractCreated(ContractCreatedEvent event);

  void publishContractAccepted(ContractAcceptedEvent event);

  void publishContractRejected(ContractRejectedEvent event);

  void publishContractCompleted(ContractCompletedEvent event);

  void publishContractCancelled(ContractCancelledEvent event);

  void publishReviewCreated(ReviewCreatedEvent event);

  void publishContractCompensation(ContractCompensationRequestedEvent event);
}
