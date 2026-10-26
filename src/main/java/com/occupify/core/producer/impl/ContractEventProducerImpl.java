package com.occupify.core.producer.impl;

import com.occupify.core.event.ContractAcceptedEvent;
import com.occupify.core.event.ContractCompensationRequestedEvent;
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

  @Value("${app.rabbitmq.contract-accepted-routing-key:contract.accepted}")
  private String contractAcceptedRoutingKey;

  @Value("${app.rabbitmq.contract-rejected-routing-key:contract.rejected}")
  private String contractRejectedRoutingKey;

  @Value("${app.rabbitmq.contract-compensation-publish-routing-key:contract.compensation.requested}")
  private String contractCompensationPublishRoutingKey;

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
  public void publishContractCompensation(ContractCompensationRequestedEvent event) {
    log.info(
        "[Event Producer] Publishing ContractCompensationRequestedEvent for contract [{}] to exchange [{}] with routingKey [{}]",
        event.contractId(),
        contractExchange,
        contractCompensationPublishRoutingKey);
    rabbitTemplate.convertAndSend(contractExchange, contractCompensationPublishRoutingKey, event);
  }
}
