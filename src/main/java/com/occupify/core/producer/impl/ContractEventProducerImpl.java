package com.occupify.core.producer.impl;

import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractCancelledEvent;
import com.occupify.core.event.ContractCompensationRequestedEvent;
import com.occupify.core.event.ContractCompletedEvent;
import com.occupify.core.event.ContractCreatedEvent;
import com.occupify.core.event.ContractRejectedEvent;
import com.occupify.core.producer.ContractEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ContractEventProducerImpl implements ContractEventProducer {

  private final RabbitTemplate rabbitTemplate;

  @Value("${app.rabbitmq.contract-exchange:occupify.contract.exchange}")
  private String contractExchange;

  @Value("${app.rabbitmq.contract-created-routing-key:contract.created}")
  private String contractCreatedRoutingKey;

  @Value("${app.rabbitmq.contract-accepted-routing-key:contract.accepted}")
  private String contractAcceptedRoutingKey;

  @Value("${app.rabbitmq.contract-rejected-routing-key:contract.rejected}")
  private String contractRejectedRoutingKey;

  @Value("${app.rabbitmq.contract-completed-routing-key:contract.completed}")
  private String contractCompletedRoutingKey;

  @Value("${app.rabbitmq.contract-cancelled-routing-key:contract.cancelled}")
  private String contractCancelledRoutingKey;

  @Value(
      "${app.rabbitmq.contract-compensation-publish-routing-key:contract.compensation.requested}")
  private String contractCompensationPublishRoutingKey;

  @Override
  public void publishContractCreated(ContractCreatedEvent event) {
    log.info(
        "[Event Producer] Publishing ContractCreatedEvent for contract [{}] to exchange [{}] with routingKey [{}]",
        event.contractId(),
        contractExchange,
        contractCreatedRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractCreatedRoutingKey, event);
  }

  @Override
  public void publishContractAccepted(ContractAcceptedEvent event) {
    log.info(
        "[Event Producer] Publishing ContractAcceptedEvent for contract [{}] to exchange [{}] with routingKey [{}]",
        event.contractId(),
        contractExchange,
        contractAcceptedRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractAcceptedRoutingKey, event);
  }

  @Override
  public void publishContractRejected(ContractRejectedEvent event) {
    log.info(
        "[Event Producer] Publishing ContractRejectedEvent for contract [{}] to exchange [{}] with routingKey [{}]",
        event.contractId(),
        contractExchange,
        contractRejectedRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractRejectedRoutingKey, event);
  }

  @Override
  public void publishContractCompleted(ContractCompletedEvent event) {
    log.info(
        "[Event Producer] Publishing ContractCompletedEvent for contract [{}] to exchange [{}] with routingKey [{}]",
        event.contractId(),
        contractExchange,
        contractCompletedRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractCompletedRoutingKey, event);
  }

  @Override
  public void publishContractCancelled(ContractCancelledEvent event) {
    log.info(
        "[Event Producer] Publishing ContractCancelledEvent for contract [{}] (cancelled by {}) to exchange [{}] with routingKey [{}]",
        event.contractId(),
        event.cancelledByRole(),
        contractExchange,
        contractCancelledRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractCancelledRoutingKey, event);
  }

  @Override
  public void publishContractCompensation(ContractCompensationRequestedEvent event) {
    log.info(
        "[Event Producer] Publishing ContractCompensationRequestedEvent for contract [{}] to exchange [{}] with routingKey [{}]",
        event.contractId(),
        contractExchange,
        contractCompensationPublishRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractCompensationPublishRoutingKey, event);
  }
}
