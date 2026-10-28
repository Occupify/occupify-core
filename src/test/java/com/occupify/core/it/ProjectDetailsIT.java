package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.repository.user.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
public class ProjectDetailsIT extends AbstractIntegrationIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private User employer;

  @BeforeEach
  void setUp() {
    truncateTables();
    employer =
        userRepository.save(
            User.builder()
                .email("it.employer@occupify.local")
                .fullName("IT Employer")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
  }

  @AfterEach
  void tearDown() {
    truncateTables();
  }

  @Test
  void duplicateTagsRejectedOnPostgres() throws Exception {
    String payload =
        """
        {
          "title": "Dự án trùng tags trên Postgres",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "tags": ["Golang", "Golang"]
        }
        """;

    mockMvc
        .perform(
            post(RouteConstants.PROJECTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.statusCode").value(400));
  }

  @Test
  void restListQueryBudget() throws Exception {
    // Seed 5 projects with jobs, tags, skills
    for (int i = 0; i < 5; i++) {
      UUID pId = UUID.randomUUID();
      jdbcTemplate.update(
          """
          INSERT INTO projects (id, employer_id, title, description, start_price, period, status, created_at, updated_at, version)
          VALUES (?, ?, ?, 'Desc', 10000000, 'MONTHLY', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
          """,
          pId,
          employer.getId(),
          "Budget Project " + i);

      jdbcTemplate.update("INSERT INTO project_tag (project_id, tag) VALUES (?, 'Tag1')", pId);
      jdbcTemplate.update("INSERT INTO project_tag (project_id, tag) VALUES (?, 'Tag2')", pId);

      UUID jId = UUID.randomUUID();
      jdbcTemplate.update(
          """
          INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, created_at, updated_at, version)
          VALUES (?, ?, 'Job Title', 'Job Desc', 1, 'MONTHLY', 10000000, 20000000, 'RECRUITING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
          """,
          jId,
          pId);

      jdbcTemplate.update(
          "INSERT INTO job_requirement (job_id, position, text) VALUES (?, 0, 'Req A')", jId);
    }

    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();

    mockMvc
        .perform(get(RouteConstants.PROJECTS).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200));

    long queryCount = statistics.getPrepareStatementCount();
    assertThat(queryCount)
        .withFailMessage("Query count %d exceeded budget of 6", queryCount)
        .isLessThanOrEqualTo(6L);
  }

  @Test
  void versionBumpsOnFieldAndCollectionChange() throws Exception {
    String createPayload =
        """
        {
          "title": "Version Test Project",
          "description": "Initial Desc",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "tags": ["Java"]
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
    long initialVersion = createJson.get("data").get("version").asLong();
    assertThat(initialVersion).isEqualTo(0L);

    // Update field
    String updatePayload =
        """
        {
          "title": "Version Test Project Updated",
          "description": "Updated Desc",
          "startPrice": 12000000,
          "period": "MONTHLY",
          "tags": ["Java"]
        }
        """;

    MvcResult updateResult =
        mockMvc
            .perform(
                put(RouteConstants.PROJECTS + "/" + projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", employer.getId().toString())
                    .header("X-User-Role", "USER")
                    .content(updatePayload))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode updateJson = objectMapper.readTree(updateResult.getResponse().getContentAsString());
    long versionAfterField = updateJson.get("data").get("version").asLong();
    assertThat(versionAfterField).isGreaterThan(initialVersion);

    // Update collection (tags)
    String updateTagsPayload =
        """
        {
          "title": "Version Test Project Updated",
          "description": "Updated Desc",
          "startPrice": 12000000,
          "period": "MONTHLY",
          "tags": ["Java", "Spring"]
        }
        """;

    MvcResult updateTagsResult =
        mockMvc
            .perform(
                put(RouteConstants.PROJECTS + "/" + projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", employer.getId().toString())
                    .header("X-User-Role", "USER")
                    .content(updateTagsPayload))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode updateTagsJson =
        objectMapper.readTree(updateTagsResult.getResponse().getContentAsString());
    long versionAfterTags = updateTagsJson.get("data").get("version").asLong();
    assertThat(versionAfterTags).isGreaterThan(versionAfterField);
  }
}
