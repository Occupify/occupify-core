package com.occupify.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class NotiEventCompatibilityTest {

  @Autowired private MessageConverter messageConverter;

  @Test
  void projectEventsReadableWithPlainJavaTimeMapper() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID employerId = UUID.randomUUID();
    Instant now = Instant.parse("2026-10-27T14:30:45Z");

    ProjectCreatedEvent createdEvent =
        new ProjectCreatedEvent(
            projectId,
            "Dự án Thiết kế UI/UX",
            "Mô tả dự án",
            "OPEN",
            employerId,
            "employer@example.com",
            now);

    org.springframework.amqp.core.Message createdAmqpMsg =
        messageConverter.toMessage(
            createdEvent, new org.springframework.amqp.core.MessageProperties());
    String createdJson =
        new String(createdAmqpMsg.getBody(), java.nio.charset.StandardCharsets.UTF_8);
    JsonNode tree = new ObjectMapper().readTree(createdJson);

    // AC-4: Must be ISO-8601 string, never an array
    assertThat(tree.get("createdAt").isTextual())
        .withFailMessage(
            "createdAt must be serialized as ISO string, but was %s", tree.get("createdAt"))
        .isTrue();
    assertThat(tree.get("createdAt").asText()).isEqualTo("2026-10-27T14:30:45Z");

    // Notification consumer compatibility
    ObjectMapper notiConsumerMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    ProjectCreatedEvent deserializedCreated =
        notiConsumerMapper.readValue(createdJson, ProjectCreatedEvent.class);
    assertThat(deserializedCreated.projectId()).isEqualTo(projectId);
    assertThat(deserializedCreated.createdAt()).isEqualTo(now);

    ProjectClosedEvent closedEvent =
        new ProjectClosedEvent(
            projectId,
            "Dự án Thiết kế UI/UX",
            employerId,
            "employer@example.com",
            employerId,
            "USER",
            now);

    org.springframework.amqp.core.Message closedAmqpMsg =
        messageConverter.toMessage(
            closedEvent, new org.springframework.amqp.core.MessageProperties());
    String closedJson =
        new String(closedAmqpMsg.getBody(), java.nio.charset.StandardCharsets.UTF_8);
    JsonNode closedTree = new ObjectMapper().readTree(closedJson);
    assertThat(closedTree.get("closedAt").isTextual())
        .withFailMessage(
            "closedAt must be serialized as ISO string, but was %s", closedTree.get("closedAt"))
        .isTrue();

    ProjectClosedEvent deserializedClosed =
        notiConsumerMapper.readValue(closedJson, ProjectClosedEvent.class);
    assertThat(deserializedClosed.projectId()).isEqualTo(projectId);
    assertThat(deserializedClosed.closedAt()).isEqualTo(now);
  }
}
