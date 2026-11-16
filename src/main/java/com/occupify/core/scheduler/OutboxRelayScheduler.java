package com.occupify.core.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.entity.outbox.OutboxEvent;
import com.occupify.core.entity.outbox.OutboxStatus;
import com.occupify.core.event.ReportCreatedEvent;
import com.occupify.core.repository.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

  private static final int MAX_RETRY_COUNT = 5;

  private final OutboxEventRepository outboxEventRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.rabbitmq.report-exchange:occupify.report.exchange}")
  private String reportExchange;

  @Scheduled(fixedDelay = 3000)
  public void processPendingOutboxEvents() {
    List<OutboxEvent> pendingEvents =
        outboxEventRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus.PENDING, MAX_RETRY_COUNT);

    if (pendingEvents.isEmpty()) {
      return;
    }

    log.debug("Processing {} pending outbox event(s)", pendingEvents.size());

    for (OutboxEvent event : pendingEvents) {
      relayEvent(event);
    }
  }

  public void relayDirectly(OutboxEvent event) {
    relayEvent(event);
  }

  private void relayEvent(OutboxEvent event) {
    try {
      Object payloadToSend;
      if ("REPORT_CREATED".equalsIgnoreCase(event.getEventType())) {
        try {
          payloadToSend = objectMapper.readValue(event.getPayload(), ReportCreatedEvent.class);
        } catch (Exception parseEx) {
          log.warn(
              "Could not deserialize OutboxEvent [{}] to ReportCreatedEvent, sending raw payload",
              event.getId());
          payloadToSend = event.getPayload();
        }
      } else {
        payloadToSend = event.getPayload();
      }

      rabbitTemplate.convertAndSend(reportExchange, event.getRoutingKey(), payloadToSend);
      event.setStatus(OutboxStatus.SENT);
      event.setSentAt(Instant.now());
      outboxEventRepository.save(event);
      log.debug(
          "Successfully relayed outbox event [{}] to exchange [{}] with routing key [{}]",
          event.getId(),
          reportExchange,
          event.getRoutingKey());
    } catch (Exception e) {
      log.error("Failed to relay outbox event [{}]: {}", event.getId(), e.getMessage(), e);
      int updatedRetryCount = event.getRetryCount() + 1;
      event.setRetryCount(updatedRetryCount);
      if (updatedRetryCount >= MAX_RETRY_COUNT) {
        event.setStatus(OutboxStatus.FAILED);
      }
      outboxEventRepository.save(event);
    }
  }
}
