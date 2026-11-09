package com.occupify.core.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.entity.outbox.OutboxEvent;
import com.occupify.core.entity.outbox.OutboxStatus;
import com.occupify.core.repository.outbox.OutboxEventRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private RabbitTemplate rabbitTemplate;

  @org.mockito.Spy
  private com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new com.fasterxml.jackson.databind.ObjectMapper();

  @InjectMocks private OutboxRelayScheduler scheduler;

  private static final String REPORT_EXCHANGE = "occupify.report.exchange";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(scheduler, "reportExchange", REPORT_EXCHANGE);
  }

  @Test
  @DisplayName("Should successfully relay pending events to RabbitMQ and mark as SENT")
  void processPendingOutboxEvents_Success() {
    OutboxEvent event =
        OutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateType("REPORT")
            .aggregateId(UUID.randomUUID())
            .eventType("REPORT_CREATED")
            .routingKey("report.created")
            .payload("{\"test\":\"data\"}")
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();

    when(outboxEventRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus.PENDING, 5))
        .thenReturn(List.of(event));

    scheduler.processPendingOutboxEvents();

    verify(rabbitTemplate)
        .convertAndSend(
            eq(REPORT_EXCHANGE),
            eq("report.created"),
            any(com.occupify.core.event.ReportCreatedEvent.class));
    assertEquals(OutboxStatus.SENT, event.getStatus());
    assertNotNull(event.getSentAt());
    verify(outboxEventRepository).save(event);
  }

  @Test
  @DisplayName("Should increment retryCount on RabbitMQ publish failure")
  void processPendingOutboxEvents_FailureRetry() {
    OutboxEvent event =
        OutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateType("REPORT")
            .aggregateId(UUID.randomUUID())
            .eventType("REPORT_CREATED")
            .routingKey("report.created")
            .payload("{\"test\":\"data\"}")
            .status(OutboxStatus.PENDING)
            .retryCount(1)
            .createdAt(Instant.now())
            .build();

    when(outboxEventRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus.PENDING, 5))
        .thenReturn(List.of(event));

    doThrow(new RuntimeException("RabbitMQ connection failure"))
        .when(rabbitTemplate)
        .convertAndSend(REPORT_EXCHANGE, "report.created", "{\"test\":\"data\"}");

    scheduler.processPendingOutboxEvents();

    assertEquals(2, event.getRetryCount());
    assertEquals(OutboxStatus.PENDING, event.getStatus());
    verify(outboxEventRepository).save(event);
  }

  @Test
  @DisplayName("Should mark status as FAILED when retryCount reaches threshold")
  void processPendingOutboxEvents_FailureMaxRetriesReached() {
    OutboxEvent event =
        OutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateType("REPORT")
            .aggregateId(UUID.randomUUID())
            .eventType("REPORT_CREATED")
            .routingKey("report.created")
            .payload("{\"test\":\"data\"}")
            .status(OutboxStatus.PENDING)
            .retryCount(4)
            .createdAt(Instant.now())
            .build();

    when(outboxEventRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus.PENDING, 5))
        .thenReturn(List.of(event));

    doThrow(new RuntimeException("RabbitMQ connection failure"))
        .when(rabbitTemplate)
        .convertAndSend(REPORT_EXCHANGE, "report.created", "{\"test\":\"data\"}");

    scheduler.processPendingOutboxEvents();

    assertEquals(5, event.getRetryCount());
    assertEquals(OutboxStatus.FAILED, event.getStatus());
    verify(outboxEventRepository).save(event);
  }

  @Test
  @DisplayName("Should relay directly when relayDirectly is invoked")
  void relayDirectly_Success() {
    OutboxEvent event =
        OutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateType("REPORT")
            .aggregateId(UUID.randomUUID())
            .eventType("REPORT_CREATED")
            .routingKey("report.created")
            .payload("{\"test\":\"data\"}")
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();

    scheduler.relayDirectly(event);

    verify(rabbitTemplate)
        .convertAndSend(
            eq(REPORT_EXCHANGE),
            eq("report.created"),
            any(com.occupify.core.event.ReportCreatedEvent.class));
    assertEquals(OutboxStatus.SENT, event.getStatus());
    assertNotNull(event.getSentAt());
    verify(outboxEventRepository).save(event);
  }

  @Test
  @DisplayName("Should do nothing when there are no pending events")
  void processPendingOutboxEvents_EmptyList() {
    when(outboxEventRepository.findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus.PENDING, 5))
        .thenReturn(Collections.emptyList());

    scheduler.processPendingOutboxEvents();

    verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(Object.class));
    verify(outboxEventRepository, never()).save(any());
  }
}
