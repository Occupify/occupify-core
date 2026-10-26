package com.occupify.core.listener;

import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.event.ContractEscrowLockedEvent;
import com.occupify.core.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "${app.rabbitmq.contract-core-queue:occupify.core.contract.queue}")
public class ContractEventListener {

  private final ContractService contractService;

  @RabbitHandler
  public void handleCompensationRequested(ContractCompensationRequestedEvent event) {
    if (event == null || event.contractId() == null) {
      log.warn("[Event Listener] Received invalid ContractCompensationRequestedEvent: {}", event);
      return;
    }

    log.warn(
        "[Saga Event Listener] Received compensation request for contract [{}] due to failure in [{}]: {}",
        event.contractId(),
        event.failedService(),
        event.reason());

    contractService.compensateContract(event.contractId(), event.reason());
  }

  @RabbitHandler
  public void handleEscrowLocked(ContractEscrowLockedEvent event) {
    if (event == null || event.contractId() == null) {
      log.warn("[Event Listener] Received invalid ContractEscrowLockedEvent: {}", event);
      return;
    }

    log.info(
        "[Saga Event Listener] Received escrow locked confirmation for contract [{}]",
        event.contractId());
    contractService.confirmContractEscrow(event.contractId());
  }
}
