package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.occupify.core.service.ProjectService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;

public class AgentRpcContractIT extends AbstractIntegrationIT {

  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private ObjectMapper objectMapper;
  @SpyBean private ProjectService projectService;

  private static final String FIXTURE_DIR = "src/test/resources/contracts/agent-rpc/";
  private static final UUID KNOWN_PROJECT_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID SPARSE_PROJECT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  @BeforeEach
  void setUp() {
    truncateTables();
    seedContractFixtureData();
  }

  private void seedContractFixtureData() {
    UUID employerId =
        UUID.nameUUIDFromBytes("occupify-seed-employer-0".getBytes(StandardCharsets.UTF_8));
    jdbcTemplate.update(
        """
        INSERT INTO users (id, email, full_name, role, status, created_at, updated_at)
        VALUES (?, 'seed-employer-0@example.com', 'Seed Employer 0', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING
        """,
        employerId);

    // Standard sample project
    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, due_date, status, hours_per_day, location, duration, version, created_at, updated_at)
        VALUES (?, ?, 'Sample Mobile Application', 'A sample mobile application for contract testing', 30000000, 'MONTHLY', '2026-12-31', 'OPEN', 8.0, 'Hà Nội', '3 tháng', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING
        """,
        KNOWN_PROJECT_ID,
        employerId);

    jdbcTemplate.update(
        "INSERT INTO project_tag (project_id, tag) VALUES (?, 'Mobile App') ON CONFLICT DO NOTHING",
        KNOWN_PROJECT_ID);
    jdbcTemplate.update(
        "INSERT INTO project_tag (project_id, tag) VALUES (?, 'F&B') ON CONFLICT DO NOTHING",
        KNOWN_PROJECT_ID);

    UUID jobId = UUID.fromString("11111111-1111-1111-1111-111111111112");
    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'React Native Specialist', 'Job requirements for mobile app', 1, 'MONTHLY', 20000000, 30000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING
        """,
        jobId,
        KNOWN_PROJECT_ID);

    jdbcTemplate.update(
        "INSERT INTO job_requirement (job_id, position, text) VALUES (?, 0, 'React Native 0.76') ON CONFLICT DO NOTHING",
        jobId);
    jdbcTemplate.update(
        "INSERT INTO job_requirement (job_id, position, text) VALUES (?, 1, 'TypeScript 5') ON CONFLICT DO NOTHING",
        jobId);

    // Sparse project
    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, due_date, status, hours_per_day, location, duration, version, created_at, updated_at)
        VALUES (?, ?, 'Sparse Sample Project', 'Sparse description', 10000000, 'MONTHLY', '2026-12-31', 'OPEN', NULL, NULL, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING
        """,
        SPARSE_PROJECT_ID,
        employerId);

    UUID sparseJobId = UUID.fromString("22222222-2222-2222-2222-222222222223");
    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Junior Developer', 'Sparse job', 1, 'MONTHLY', 8000000, 12000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING
        """,
        sparseJobId,
        SPARSE_PROJECT_ID);
  }

  private JsonNode sendRpc(String routingKey, String jsonBody) {
    MessageProperties props = new MessageProperties();
    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    props.setCorrelationId(UUID.randomUUID().toString());
    props.setHeader("x-trace-id", "trace-contract-" + UUID.randomUUID());
    Message message = new Message(jsonBody.getBytes(StandardCharsets.UTF_8), props);
    Message reply = rabbitTemplate.sendAndReceive("occupify.agent.rpc", routingKey, message);
    assertThat(reply).isNotNull();
    try {
      return objectMapper.readTree(reply.getBody());
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to parse RPC response: " + new String(reply.getBody(), StandardCharsets.UTF_8),
          e);
    }
  }

  private void normalizeTimestamps(JsonNode node) {
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = entry.getKey();
        if ("createdAt".equals(key) || "updatedAt".equals(key) || "closedAt".equals(key)) {
          if (!entry.getValue().isNull()) {
            obj.put(key, "<ts>");
          }
        } else {
          normalizeTimestamps(entry.getValue());
        }
      }
    } else if (node.isArray()) {
      ArrayNode array = (ArrayNode) node;
      for (JsonNode elem : array) {
        normalizeTimestamps(elem);
      }
    }
  }

  private void handleFixture(String filename, JsonNode reply) throws Exception {
    normalizeTimestamps(reply);
    boolean isRecord = Boolean.getBoolean("contracts.record");
    Path fixturePath = Paths.get(FIXTURE_DIR, filename);

    if (isRecord) {
      Files.createDirectories(fixturePath.getParent());
      String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reply);
      Files.writeString(fixturePath, prettyJson + "\n", StandardCharsets.UTF_8);
      Path targetClassesPath = Paths.get("target/test-classes/contracts/agent-rpc", filename);
      if (Files.exists(targetClassesPath.getParent())) {
        Files.writeString(targetClassesPath, prettyJson + "\n", StandardCharsets.UTF_8);
      }
    } else {
      JsonNode expected;
      ClassPathResource resource = new ClassPathResource("contracts/agent-rpc/" + filename);
      if (resource.exists()) {
        try (InputStream is = resource.getInputStream()) {
          expected = objectMapper.readTree(is);
        }
      } else if (Files.exists(fixturePath)) {
        expected = objectMapper.readTree(Files.readString(fixturePath, StandardCharsets.UTF_8));
      } else {
        throw new AssertionError(
            "Fixture file "
                + filename
                + " does not exist. Run with -Dcontracts.record=true to record it.");
      }
      assertThat(reply).isEqualTo(expected);
    }
  }

  @Test
  void repliesMatchFixtures() throws Exception {
    // 1. search.json
    JsonNode searchReply =
        sendRpc(
            "core.project.search",
            "{\"keyword\":\"Sample\",\"sort\":\"title,asc\",\"page\":0,\"size\":5}");
    handleFixture("search.json", searchReply);

    // 2. get.json
    JsonNode getReply = sendRpc("core.project.get", "{\"projectId\":\"" + KNOWN_PROJECT_ID + "\"}");
    handleFixture("get.json", getReply);

    // 3. get-sparse.json
    JsonNode sparseReply =
        sendRpc("core.project.get", "{\"projectId\":\"" + SPARSE_PROJECT_ID + "\"}");
    handleFixture("get-sparse.json", sparseReply);

    // 4. get-404.json
    JsonNode notFoundReply =
        sendRpc("core.project.get", "{\"projectId\":\"00000000-0000-0000-0000-000000000000\"}");
    handleFixture("get-404.json", notFoundReply);

    // 5. get-400.json
    JsonNode badArgReply = sendRpc("core.project.get", "{\"projectId\":\"not-a-uuid\"}");
    handleFixture("get-400.json", badArgReply);

    // 6. get-500.json (simulate fixed 500 error reply)
    UUID failId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    Mockito.doThrow(new RuntimeException("Simulated unexpected internal error"))
        .when(projectService)
        .getAgentProject(failId);
    try {
      JsonNode errReply = sendRpc("core.project.get", "{\"projectId\":\"" + failId + "\"}");
      handleFixture("get-500.json", errReply);
    } finally {
      Mockito.reset(projectService);
    }
  }
}
