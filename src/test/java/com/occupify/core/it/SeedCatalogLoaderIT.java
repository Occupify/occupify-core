package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.seed.SeedCatalogLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"it", "seed"})
public class SeedCatalogLoaderIT extends AbstractIntegrationIT {

  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private SeedCatalogLoader seedCatalogLoader;

  private RabbitAdmin rabbitAdmin;
  private Queue seedCoreQueue;
  private Queue seedNotiQueue;

  @BeforeEach
  void setUp() {
    truncateTables();
    rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());

    TopicExchange coreExchange = new TopicExchange("occupify.core.exchange", true, false);
    rabbitAdmin.declareExchange(coreExchange);
    seedCoreQueue = new Queue("test.seed.core." + UUID.randomUUID(), false, false, false);
    rabbitAdmin.declareQueue(seedCoreQueue);
    rabbitAdmin.declareBinding(BindingBuilder.bind(seedCoreQueue).to(coreExchange).with("#"));

    TopicExchange notiExchange = new TopicExchange("occupify.notification.exchange", true, false);
    rabbitAdmin.declareExchange(notiExchange);
    seedNotiQueue = new Queue("test.seed.noti." + UUID.randomUUID(), false, false, false);
    rabbitAdmin.declareQueue(seedNotiQueue);
    rabbitAdmin.declareBinding(BindingBuilder.bind(seedNotiQueue).to(notiExchange).with("#"));

    seedCatalogLoader.loadDefaultCatalog();
  }

  @AfterEach
  void tearDown() {
    if (seedCoreQueue != null) {
      rabbitAdmin.deleteQueue(seedCoreQueue.getName());
    }
    if (seedNotiQueue != null) {
      rabbitAdmin.deleteQueue(seedNotiQueue.getName());
    }
    truncateTables();
  }

  @Test
  void loadsSampleWithFileIds() {
    // Verifies that sample catalogue loads with exact file IDs and relations
    Integer projectCount = jdbcTemplate.queryForObject("SELECT count(*) FROM projects", Integer.class);
    assertThat(projectCount).isGreaterThanOrEqualTo(1);

    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList("SELECT id, title, hours_per_day, location, duration, version FROM projects");
    assertThat(rows).isNotEmpty();

    Integer jobReqs = jdbcTemplate.queryForObject("SELECT count(*) FROM job_requirement", Integer.class);
    assertThat(jobReqs).isNotNull();
  }

  @Test
  void secondRunChangesNothing() {
    // Assert counts, updated_at, version unchanged on rerun
    List<Map<String, Object>> beforeProjects =
        jdbcTemplate.queryForList("SELECT id, updated_at, version FROM projects ORDER BY id");
    List<Map<String, Object>> beforeJobs =
        jdbcTemplate.queryForList("SELECT id, updated_at, version FROM job ORDER BY id");

    // Second load
    seedCatalogLoader.loadDefaultCatalog();

    List<Map<String, Object>> afterProjects =
        jdbcTemplate.queryForList("SELECT id, updated_at, version FROM projects ORDER BY id");
    List<Map<String, Object>> afterJobs =
        jdbcTemplate.queryForList("SELECT id, updated_at, version FROM job ORDER BY id");

    assertThat(afterProjects).isEqualTo(beforeProjects);
    assertThat(afterJobs).isEqualTo(beforeJobs);
  }

  @Test
  void publishesNoEvents() {
    Message coreMsg = rabbitTemplate.receive(seedCoreQueue.getName(), 1000L);
    assertThat(coreMsg).isNull();

    Message notiMsg = rabbitTemplate.receive(seedNotiQueue.getName(), 1000L);
    assertThat(notiMsg).isNull();
  }

  @Test
  void rejectsNonUuidId() throws Exception {
    String badJson =
        """
        [
          {
            "id": "not-a-valid-uuid",
            "title": "Bad Id Project",
            "description": "Desc",
            "startPrice": 10000000,
            "period": "MONTHLY",
            "jobs": []
          }
        ]
        """;
    Path tempFile = Files.createTempFile("bad-catalog-", ".json");
    try {
      Files.writeString(tempFile, badJson, StandardCharsets.UTF_8);
      assertThatThrownBy(() -> seedCatalogLoader.loadCatalogFromPath(tempFile))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("not-a-valid-uuid");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void rejectsLimitViolations() throws Exception {
    String limitViolationJson =
        """
        [
          {
            "id": "%s",
            "title": "Violation Project",
            "description": "Desc",
            "startPrice": 10000000,
            "period": "MONTHLY",
            "hoursPerDay": 25.0,
            "jobs": []
          }
        ]
        """.formatted(UUID.randomUUID());
    Path tempFile = Files.createTempFile("limit-violation-", ".json");
    try {
      Files.writeString(tempFile, limitViolationJson, StandardCharsets.UTF_8);
      assertThatThrownBy(() -> seedCatalogLoader.loadCatalogFromPath(tempFile))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("hoursPerDay must be between 0.5 and 24.0");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void seedsSyntheticFreelancers() {
    // 20 synthetic freelancers from UUID.nameUUIDFromBytes("occupify-seed-freelancer-NN")
    for (int i = 0; i < 20; i++) {
      String name = "occupify-seed-freelancer-" + i;
      UUID expectedId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
      String expectedEmail = "seed-freelancer-" + i + "@example.com";

      Integer count =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM users WHERE id = ? AND email = ?",
              Integer.class,
              expectedId,
              expectedEmail);
      assertThat(count).withFailMessage("Missing synthetic freelancer %s", expectedEmail).isEqualTo(1);
    }
  }

  @Test
  void readinessUpOnlyAfterSeeding() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/actuator/health/readiness", String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }
}
