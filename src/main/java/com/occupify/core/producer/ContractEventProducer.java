package com.occupify.core.producer;

import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.event.ContractRejectedEvent;

public interface ContractEventProducer {
  void publishContractAccepted(ContractAcceptedEvent event);

  void publishContractRejected(ContractRejectedEvent event);

  void publishContractCompensation(ContractCompensationRequestedEvent event);
}
