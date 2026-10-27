package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.service.StorageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class AbstractIntegrationIT {

  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("occupify_core")
          .withUsername("postgres")
          .withPassword("postgres");

  protected static final RabbitMQContainer RABBITMQ =
      new RabbitMQContainer("rabbitmq:3.13-management-alpine");

  static {
    POSTGRES.start();
    RABBITMQ.start();
  }

  @DynamicPropertySource
  static void configureContainers(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
    registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
  }

  @Autowired protected JdbcTemplate jdbcTemplate;

  @MockBean protected StorageService mockStorageService;

  @Test
  void smokeCheckMockBean() {
    assertThat(mockStorageService).isNotNull();
  }

  protected void truncateTables() {
    List<String> tables =
        List.of(
            "review_evidences",
            "reviews",
            "report_evidences",
            "reports",
            "cancellation_contracts",
            "contract_completion_confirmations",
            "contracts",
            "offers",
            "bids",
            "job_skills",
            "job_requirement",
            "job",
            "project_skills",
            "project_tag",
            "projects");
    for (String table : tables) {
      try {
        jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
      } catch (Exception ignored) {
        // Table may not exist yet in earlier migration states
      }
    }
  }
}
