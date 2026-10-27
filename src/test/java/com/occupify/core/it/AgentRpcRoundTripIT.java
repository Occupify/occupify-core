package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.service.ProjectService;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

public class AgentRpcRoundTripIT extends AbstractIntegrationIT {

  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @SpyBean private ProjectService projectService;

  private UUID testEmployerId;
  private UUID testProjectId;
  private UUID testJobId;

  @BeforeEach
  void setUp() {
    truncateTables();
    testEmployerId = UUID.randomUUID();
    testProjectId = UUID.randomUUID();
    testJobId = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO users (id, email, full_name, role, status, created_at, updated_at)
        VALUES (?, 'rpc.employer@example.com', 'RPC Employer', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        testEmployerId);

    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, due_date, status, hours_per_day, location, duration, version, created_at, updated_at)
        VALUES (?, ?, 'RPC Sample Project', 'Project Description', 20000000, 'MONTHLY', '2026-12-31', 'OPEN', 7.5, 'Đà Nẵng', '3 tháng', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        testProjectId,
        testEmployerId);

    jdbcTemplate.update("INSERT INTO project_tag (project_id, tag) VALUES (?, 'Fintech')", testProjectId);
    jdbcTemplate.update("INSERT INTO project_tag (project_id, tag) VALUES (?, 'AI')", testProjectId);

    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Backend Developer', 'Job Requirements Description', 1, 'MONTHLY', 15000000, 25000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        testJobId,
        testProjectId);

    jdbcTemplate.update("INSERT INTO job_requirement (job_id, position, text) VALUES (?, 0, 'Java 25')", testJobId);
    jdbcTemplate.update("INSERT INTO job_requirement (job_id, position, text) VALUES (?, 1, 'PostgreSQL')", testJobId);
  }

  @AfterEach
  void tearDown() {
    truncateTables();
  }

  private JsonNode sendRpc(String routingKey, String jsonBody) {
    MessageProperties props = new MessageProperties();
    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    props.setCorrelationId(UUID.randomUUID().toString());
    props.setHeader("x-trace-id", "trace-" + UUID.randomUUID());
    Message message = new Message(jsonBody.getBytes(StandardCharsets.UTF_8), props);
    Message reply = rabbitTemplate.sendAndReceive("occupify.agent.rpc", routingKey, message);
    assertThat(reply)
        .withFailMessage("Expected reply on %s but received null (timeout or dropped)", routingKey)
        .isNotNull();
    try {
      return objectMapper.readTree(reply.getBody());
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse RPC response: " + new String(reply.getBody(), StandardCharsets.UTF_8), e);
    }
  }

  @Test
  void searchReturnsIsoDatesRequirementsAndVersion() {
    JsonNode reply =
        sendRpc("core.project.search", "{\"keyword\":\"Sample\",\"sort\":\"createdAt,desc\",\"page\":0,\"size\":5}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(200);

    JsonNode data = reply.get("data");
    assertThat(data.isArray()).isTrue();
    assertThat(data.size()).isGreaterThanOrEqualTo(1);

    JsonNode first = data.get(0);
    assertThat(first.get("version").asLong()).isEqualTo(0L);

    String createdAt = first.get("createdAt").asText();
    assertThat(createdAt).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?$");

    String dueDate = first.get("dueDate").asText();
    assertThat(dueDate).matches("^\\d{4}-\\d{2}-\\d{2}$");

    JsonNode jobs = first.get("jobs");
    assertThat(jobs.isArray()).isTrue();
    JsonNode firstJob = jobs.get(0);
    assertThat(firstJob.get("requirements").isArray()).isTrue();
    assertThat(firstJob.get("requirements").get(0).asText()).isEqualTo("Java 25");
  }

  @Test
  void getUnknownReturns404() {
    UUID unknownId = UUID.randomUUID();
    JsonNode reply = sendRpc("core.project.get", "{\"projectId\":\"" + unknownId + "\"}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(404);
    assertThat(reply.get("errorCode").asText()).isEqualTo("PROJ_001");
  }

  @Test
  void getBadUuidReturns400Fast() {
    long start = System.currentTimeMillis();
    JsonNode reply = sendRpc("core.project.get", "{\"projectId\":\"not-a-uuid\"}");
    long elapsed = System.currentTimeMillis() - start;
    assertThat(elapsed).isLessThan(2000L);
    assertThat(reply.get("statusCode").asInt()).isEqualTo(400);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INVALID_ARGUMENT");
    assertThat(reply.get("message").asText()).doesNotContain("not-a-uuid");
  }

  @Test
  void getMissingIdReturns400() {
    JsonNode reply = sendRpc("core.project.get", "{}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(400);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INVALID_ARGUMENT");
  }

  @Test
  void nonJsonBodyReturns400() {
    JsonNode reply = sendRpc("core.project.get", "not a valid json string");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(400);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INVALID_ARGUMENT");
  }

  @Test
  void searchBadEmployerIdReturns400() {
    JsonNode reply = sendRpc("core.project.search", "{\"employerId\":\"not-a-uuid\"}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(400);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INVALID_ARGUMENT");
    assertThat(reply.get("message").asText()).doesNotContain("not-a-uuid");
  }

  @Test
  void searchUnknownEnumReturns400() {
    JsonNode reply = sendRpc("core.project.search", "{\"status\":\"UNKNOWN_STATUS\"}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(400);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INVALID_ARGUMENT");
  }

  @Test
  void searchUnknownSortReturns400() {
    JsonNode reply = sendRpc("core.project.search", "{\"sort\":\"maliciousColumn,asc\"}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(400);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INVALID_ARGUMENT");
  }

  @Test
  void unexpectedErrorReturnsFixed500() {
    doThrow(new RuntimeException("Sensitive database connection details"))
        .when(projectService)
        .getProjects(any(), any());

    JsonNode reply = sendRpc("core.project.search", "{\"page\":0,\"size\":5}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(500);
    assertThat(reply.get("errorCode").asText()).isEqualTo("INTERNAL_SERVER_ERROR");
    assertThat(reply.get("message").asText()).isEqualTo("Failed to process the request");
    assertThat(reply.toString()).doesNotContain("Sensitive database");
  }

  @Test
  void noEmployerAnywhere() {
    JsonNode searchReply = sendRpc("core.project.search", "{\"page\":0,\"size\":5}");
    assertThat(searchReply.get("statusCode").asInt()).isEqualTo(200);
    assertThat(searchReply.toString()).doesNotContain("\"employer\"");
    assertThat(searchReply.toString()).doesNotContain("rpc.employer@example.com");

    JsonNode getReply = sendRpc("core.project.get", "{\"projectId\":\"" + testProjectId + "\"}");
    assertThat(getReply.get("statusCode").asInt()).isEqualTo(200);
    assertThat(getReply.toString()).doesNotContain("\"employer\"");
    assertThat(getReply.toString()).doesNotContain("rpc.employer@example.com");
  }

  @Test
  void rpcSearchQueryBudget() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();

    JsonNode reply = sendRpc("core.project.search", "{\"page\":0,\"size\":10}");
    assertThat(reply.get("statusCode").asInt()).isEqualTo(200);

    long queries = statistics.getPrepareStatementCount();
    assertThat(queries)
        .withFailMessage("RPC Search exceeded query budget of 6, executed: %d", queries)
        .isLessThanOrEqualTo(6L);
  }
}
