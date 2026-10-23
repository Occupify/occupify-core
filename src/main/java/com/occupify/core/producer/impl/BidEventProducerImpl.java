package com.occupify.core.producer.impl;

import com.occupify.core.event.BidCancelledEvent;
import com.occupify.core.event.BidCreatedEvent;
import com.occupify.core.producer.BidEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BidEventProducerImpl implements BidEventProducer {

  private final RabbitTemplate rabbitTemplate;

  @Value("${app.rabbitmq.exchange:occupify.notification.exchange}")
  private String exchange;

  @Value("${app.rabbitmq.bid-created-routing-key:bid.created}")
  private String bidCreatedRoutingKey;

  @Value("${app.rabbitmq.bid-cancelled-routing-key:bid.cancelled}")
  private String bidCancelledRoutingKey;

  @Override
  public void publishBidCreated(BidCreatedEvent event) {
    log.info(
        "[Event Producer] Publishing BidCreatedEvent for bid [{}] (job [{}], freelancer [{}]) to exchange [{}] with routingKey [{}]",
        event.bidId(),
        event.jobId(),
        event.freelancerId(),
        exchange,
        bidCreatedRoutingKey);
    try {
      rabbitTemplate.convertAndSend(exchange, bidCreatedRoutingKey, event);
    } catch (Exception ex) {
      log.warn("[Event Producer] Failed to publish BidCreatedEvent: {}", ex.getMessage());
    }
  }

  @Override
  public void publishBidCancelled(BidCancelledEvent event) {
    log.info(
        "[Event Producer] Publishing BidCancelledEvent for bid [{}] (freelancer [{}]) to exchange [{}] with routingKey [{}]",
        event.bidId(),
        event.freelancerId(),
        exchange,
        bidCancelledRoutingKey);
    try {
      rabbitTemplate.convertAndSend(exchange, bidCancelledRoutingKey, event);
    } catch (Exception ex) {
      log.warn("[Event Producer] Failed to publish BidCancelledEvent: {}", ex.getMessage());
    }
  }
}
