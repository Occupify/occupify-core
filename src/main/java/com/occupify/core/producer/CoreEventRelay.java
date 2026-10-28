package com.occupify.core.producer;

import com.occupify.core.event.domain.JobStatusChangedDomainEvent;
import com.occupify.core.event.domain.ProjectClosedDomainEvent;
import com.occupify.core.event.domain.ProjectCreatedDomainEvent;
import com.occupify.core.event.domain.ProjectUpdatedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreEventRelay {

  private final RabbitTemplate rabbitTemplate;

  @Value("${app.rabbitmq.core-exchange:occupify.core.exchange}")
  private String coreExchange;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onProjectCreated(ProjectCreatedDomainEvent event) {
    publishSafely("project.created", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onProjectUpdated(ProjectUpdatedDomainEvent event) {
    publishSafely("project.updated", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onProjectClosed(ProjectClosedDomainEvent event) {
    publishSafely("project.closed", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onJobStatusChanged(JobStatusChangedDomainEvent event) {
    publishSafely("job.status.changed", event);
  }

  private void publishSafely(String routingKey, Object payload) {
    try {
      rabbitTemplate.convertAndSend(coreExchange, routingKey, payload);
      log.info(
          "[CoreEventRelay] Published {} to {} with routing key {}",
          payload.getClass().getSimpleName(),
          coreExchange,
          routingKey);
    } catch (Exception e) {
      log.warn(
          "[CoreEventRelay] Failed to publish {} to {}: {}",
          payload.getClass().getSimpleName(),
          coreExchange,
          e.getMessage());
    }
  }
}
