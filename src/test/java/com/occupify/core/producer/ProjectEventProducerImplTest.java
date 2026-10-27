package com.occupify.core.producer;

import static org.mockito.Mockito.*;

import com.occupify.core.event.ProjectClosedEvent;
import com.occupify.core.event.ProjectCreatedEvent;
import com.occupify.core.producer.impl.ProjectEventProducerImpl;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class ProjectEventProducerImplTest {

  @Mock private RabbitTemplate rabbitTemplate;

  private ProjectEventProducerImpl producer;

  @BeforeEach
  void setUp() throws Exception {
    producer = new ProjectEventProducerImpl(rabbitTemplate);
    setField(producer, "exchange", "occupify.project.exchange");
    setField(producer, "projectCreatedRoutingKey", "project.created");
    setField(producer, "projectClosedRoutingKey", "project.closed");
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  @DisplayName("Should publish ProjectCreatedEvent with correct exchange and routingKey")
  void publishProjectCreated_publishesEvent() {
    ProjectCreatedEvent event =
        new ProjectCreatedEvent(
            UUID.randomUUID(),
            "Title",
            "Description",
            "OPEN",
            UUID.randomUUID(),
            "emp@test.local",
            Instant.now());

    producer.publishProjectCreated(event);

    verify(rabbitTemplate).convertAndSend("occupify.project.exchange", "project.created", event);
  }

  @Test
  @DisplayName("Should publish ProjectClosedEvent with correct exchange and routingKey")
  void publishProjectClosed_publishesEvent() {
    ProjectClosedEvent event =
        new ProjectClosedEvent(
            UUID.randomUUID(),
            "Title",
            UUID.randomUUID(),
            "emp@test.local",
            UUID.randomUUID(),
            "EMPLOYER",
            Instant.now());

    producer.publishProjectClosed(event);

    verify(rabbitTemplate).convertAndSend("occupify.project.exchange", "project.closed", event);
  }
}
