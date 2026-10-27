package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.repository.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
public class CoreEventsIT extends AbstractIntegrationIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RabbitTemplate rabbitTemplate;

  private RabbitAdmin rabbitAdmin;
  private Queue coreEventsQueue;
  private Queue notiEventsQueue;
  private User employer;
  private User freelancer;

  @BeforeEach
  void setUp() {
    truncateTables();
    rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());

    TopicExchange coreExchange = new TopicExchange("occupify.core.exchange", true, false);
    rabbitAdmin.declareExchange(coreExchange);

    coreEventsQueue = new Queue("test.core.events.queue." + UUID.randomUUID(), false, false, false);
    rabbitAdmin.declareQueue(coreEventsQueue);
    rabbitAdmin.declareBinding(
        BindingBuilder.bind(coreEventsQueue).to(coreExchange).with("project.*"));
    rabbitAdmin.declareBinding(
        BindingBuilder.bind(coreEventsQueue).to(coreExchange).with("job.#"));

    TopicExchange notiExchange = new TopicExchange("occupify.notification.exchange", true, false);
    rabbitAdmin.declareExchange(notiExchange);

    notiEventsQueue = new Queue("test.noti.events.queue." + UUID.randomUUID(), false, false, false);
    rabbitAdmin.declareQueue(notiEventsQueue);
    rabbitAdmin.declareBinding(
        BindingBuilder.bind(notiEventsQueue).to(notiExchange).with("#"));

    employer =
        userRepository.save(
            User.builder()
                .email("employer.events@example.com")
                .fullName("Employer Events")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    freelancer =
        userRepository.save(
            User.builder()
                .email("freelancer.events@example.com")
                .fullName("Freelancer Events")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
  }

  @AfterEach
  void tearDown() {
    if (coreEventsQueue != null) {
      rabbitAdmin.deleteQueue(coreEventsQueue.getName());
    }
    if (notiEventsQueue != null) {
      rabbitAdmin.deleteQueue(notiEventsQueue.getName());
    }
    truncateTables();
  }

  private JsonNode receiveJson(String queueName, long timeoutMs) throws Exception {
    Message msg = rabbitTemplate.receive(queueName, timeoutMs);
    assertThat(msg).withFailMessage("Expected message on %s within %d ms", queueName, timeoutMs).isNotNull();
    return objectMapper.readTree(new String(msg.getBody(), StandardCharsets.UTF_8));
  }

  @Test
  void onePerChangeAfterCommit() throws Exception {
    // 1. Create project -> project.created
    String createPayload =
        """
        {
          "title": "Core Events Project",
          "description": "Description",
          "startPrice": 10000000,
          "period": "MONTHLY"
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post(RouteConstants.PROJECTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", employer.getId().toString())
                    .header("X-User-Role", "USER")
                    .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String projectId = createJson.get("data").get("id").asText();

    JsonNode createdEvent = receiveJson(coreEventsQueue.getName(), 3000L);
    assertThat(createdEvent.get("projectId").asText()).isEqualTo(projectId);
    assertThat(createdEvent.get("version").asLong()).isEqualTo(0L);
    assertThat(createdEvent.has("occurredAt")).isTrue();

    // 2. Update project -> project.updated
    String updatePayload =
        """
        {
          "title": "Core Events Project Updated",
          "description": "Updated",
          "startPrice": 12000000,
          "period": "MONTHLY"
        }
        """;

    mockMvc
        .perform(
            put(RouteConstants.PROJECTS + "/" + projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(updatePayload))
        .andExpect(status().isOk());

    JsonNode updatedEvent = receiveJson(coreEventsQueue.getName(), 3000L);
    assertThat(updatedEvent.get("projectId").asText()).isEqualTo(projectId);
    assertThat(updatedEvent.get("version").asLong()).isGreaterThanOrEqualTo(1L);

    // 3. Close project -> project.closed
    mockMvc
        .perform(
            patch(RouteConstants.PROJECTS + "/" + projectId + "/close")
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk());

    JsonNode closedEvent = receiveJson(coreEventsQueue.getName(), 3000L);
    assertThat(closedEvent.get("projectId").asText()).isEqualTo(projectId);
  }

  @Test
  void noneOnCoreExchangeOnRollback() throws Exception {
    // Invalid project creation that violates validation or causes failure
    String invalidPayload =
        """
        {
          "title": "",
          "description": "Description",
          "startPrice": 10000000,
          "period": "MONTHLY"
        }
        """;

    mockMvc.perform(
        post(RouteConstants.PROJECTS)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Id", employer.getId().toString())
            .header("X-User-Role", "USER")
            .content(invalidPayload));

    Message msg = rabbitTemplate.receive(coreEventsQueue.getName(), 1500L);
    assertThat(msg).isNull();
  }

  @Test
  void payloadHasNoPersonalDataAndHasVersion() throws Exception {
    String createPayload =
        """
        {
          "title": "Personal Data Check Project",
          "description": "Private project description with user data",
          "startPrice": 15000000,
          "period": "MONTHLY"
        }
        """;

    mockMvc
        .perform(
            post(RouteConstants.PROJECTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(createPayload))
        .andExpect(status().isCreated());

    JsonNode event = receiveJson(coreEventsQueue.getName(), 3000L);
    String jsonString = event.toString();
    assertThat(jsonString).doesNotContain("employer.events@example.com");
    assertThat(jsonString).doesNotContain("Employer Events");
    assertThat(jsonString).doesNotContain("Private project description");
    assertThat(event.has("version")).isTrue();
    assertThat(event.has("occurredAt")).isTrue();
  }

  @Test
  void notificationEventsUnchanged() throws Exception {
    String createPayload =
        """
        {
          "title": "Noti Check Project",
          "description": "Desc",
          "startPrice": 10000000,
          "period": "MONTHLY"
        }
        """;

    mockMvc
        .perform(
            post(RouteConstants.PROJECTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(createPayload))
        .andExpect(status().isCreated());

    Message notiMsg = rabbitTemplate.receive(notiEventsQueue.getName(), 3000L);
    assertThat(notiMsg).isNotNull();
    JsonNode notiJson = objectMapper.readTree(new String(notiMsg.getBody(), StandardCharsets.UTF_8));
    assertThat(notiJson.get("employerEmail").asText()).isEqualTo("employer.events@example.com");
  }

  @Test
  void jobStatusKeyMatchesJobHashBinding() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, status, version, created_at, updated_at)
        VALUES (?, ?, 'Job Status Test Project', 'Desc', 10000000, 'MONTHLY', 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        projectId,
        employer.getId());

    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Job Title', 'Desc', 1, 'MONTHLY', 5000000, 10000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        jobId,
        projectId);

    jdbcTemplate.update(
        """
        INSERT INTO contracts (id, employer_id, employee_id, job_id, title, agreed_rate, salary_type, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, 'Contract 1', 5000000, 'MONTHLY', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        contractId,
        employer.getId(),
        freelancer.getId(),
        jobId);

    // Accept contract -> Job becomes FILLED -> publishes job.status.changed to occupify.core.exchange
    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + "/" + contractId + "/accept")
                .header("X-User-Id", freelancer.getId().toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk());

    JsonNode jobStatusEvent = receiveJson(coreEventsQueue.getName(), 3000L);
    assertThat(jobStatusEvent.get("jobId").asText()).isEqualTo(jobId.toString());
    assertThat(jobStatusEvent.get("oldStatus").asText()).isEqualTo("RECRUITING");
    assertThat(jobStatusEvent.get("newStatus").asText()).isEqualTo("FILLED");
    assertThat(jobStatusEvent.has("jobVersion")).isTrue();
  }
}
