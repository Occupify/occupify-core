package com.occupify.core.producer.impl;

import com.occupify.core.event.ProjectClosedEvent;
import com.occupify.core.event.ProjectCreatedEvent;
import com.occupify.core.producer.ProjectEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProjectEventProducerImpl implements ProjectEventProducer {

  private final RabbitTemplate rabbitTemplate;

  @Value("${app.rabbitmq.exchange:occupify.notification.exchange}")
  private String exchange;

  @Value("${app.rabbitmq.project-created-routing-key:project.created}")
  private String projectCreatedRoutingKey;

  @Value("${app.rabbitmq.project-closed-routing-key:project.closed}")
  private String projectClosedRoutingKey;

  @Override
  public void publishProjectCreated(ProjectCreatedEvent event) {
    log.info(
        "[Event Producer] Publishing ProjectCreatedEvent for project [{}] ('{}') to exchange [{}] with routingKey [{}]",
        event.projectId(),
        event.title(),
        exchange,
        projectCreatedRoutingKey);
    rabbitTemplate.convertAndSend(exchange, projectCreatedRoutingKey, event);
  }

  @Override
  public void publishProjectClosed(ProjectClosedEvent event) {
    log.info(
        "[Event Producer] Publishing ProjectClosedEvent for project [{}] ('{}') to exchange [{}] with routingKey [{}]",
        event.projectId(),
        event.title(),
        exchange,
        projectClosedRoutingKey);
    rabbitTemplate.convertAndSend(exchange, projectClosedRoutingKey, event);
  }
}
