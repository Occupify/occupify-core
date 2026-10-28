package com.occupify.core.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProjectDetailsRestTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @org.springframework.boot.test.mock.mockito.MockBean
  private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

  private User employer;

  @BeforeEach
  void setUp() {
    employer =
        userRepository.saveAndFlush(
            User.builder()
                .email("employer-" + UUID.randomUUID() + "@occupify.local")
                .fullName("Test Employer")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
  }

  @Test
  void createProject_withValidNewFields_returnsCreatedWithFields() throws Exception {
    String payload =
        """
        {
          "title": "Dự án mới đầy đủ thông tin",
          "description": "Mô tả chi tiết dự án",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "hoursPerDay": 6.0,
          "location": "Đà Nẵng",
          "duration": "2 - 3 tháng",
          "tags": ["Mobile App", "F&B"]
        }
        """;

    mockMvc
        .perform(
            post(RouteConstants.PROJECTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statusCode").value(201))
        .andExpect(jsonPath("$.data.hoursPerDay").value(6.0))
        .andExpect(jsonPath("$.data.location").value("Đà Nẵng"))
        .andExpect(jsonPath("$.data.duration").value("2 - 3 tháng"))
        .andExpect(jsonPath("$.data.tags", containsInAnyOrder("F&B", "Mobile App")))
        .andExpect(jsonPath("$.data.employer").exists())
        .andExpect(jsonPath("$.data.version").value(0));
  }

  @Test
  void createProject_withInvalidHoursPerDay_returns400() throws Exception {
    String payload =
        """
        {
          "title": "Dự án sai hoursPerDay",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "hoursPerDay": 0.0
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

    String payloadOver =
        """
        {
          "title": "Dự án sai hoursPerDay lớn hơn 24",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "hoursPerDay": 24.1
        }
        """;

    mockMvc
        .perform(
            post(RouteConstants.PROJECTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(payloadOver))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.statusCode").value(400));
  }

  @Test
  void createProject_withLocationOver120Chars_returns400() throws Exception {
    String payload =
        """
        {
          "title": "Dự án sai location",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "location": "%s"
        }
        """
            .formatted("L".repeat(121));

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
  void createProject_withDurationOver60Chars_returns400() throws Exception {
    String payload =
        """
        {
          "title": "Dự án sai duration",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "duration": "%s"
        }
        """
            .formatted("D".repeat(61));

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
  void createProject_withMoreThan10Tags_returns400() throws Exception {
    String payload =
        """
        {
          "title": "Dự án sai nhiều hơn 10 tags",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "tags": ["T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11"]
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
  void createProject_withTagOver40Chars_returns400() throws Exception {
    String payload =
        """
        {
          "title": "Dự án sai tag dài",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "tags": ["%s"]
        }
        """
            .formatted("T".repeat(41));

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
  void createProject_withDuplicateTags_returns400() throws Exception {
    String payload =
        """
        {
          "title": "Dự án trùng tags",
          "description": "Mô tả",
          "startPrice": 10000000,
          "period": "MONTHLY",
          "tags": ["Mobile App", "Mobile App"]
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
  void updateProject_withNewFields_returnsUpdated() throws Exception {
    String createPayload =
        """
        {
          "title": "Dự án ban đầu",
          "description": "Mô tả",
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

    JsonNode json = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String projectId = json.get("data").get("id").asText();

    String updatePayload =
        """
        {
          "title": "Dự án đã cập nhật",
          "description": "Mô tả cập nhật",
          "startPrice": 15000000,
          "period": "MONTHLY",
          "hoursPerDay": 8.0,
          "location": "Hà Nội",
          "duration": "6 tháng",
          "tags": ["Backend", "Java"]
        }
        """;

    mockMvc
        .perform(
            put(RouteConstants.PROJECTS + "/" + projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content(updatePayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hoursPerDay").value(8.0))
        .andExpect(jsonPath("$.data.location").value("Hà Nội"))
        .andExpect(jsonPath("$.data.duration").value("6 tháng"))
        .andExpect(jsonPath("$.data.tags", containsInAnyOrder("Backend", "Java")))
        .andExpect(jsonPath("$.data.version").value(1));
  }

  @Test
  void readBackJobRequirementsAndSparseFields() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, status, created_at, updated_at, version)
        VALUES (?, ?, 'Sparse Project', 'Description', 10000000, 'MONTHLY', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
        """,
        projectId,
        employer.getId());

    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, created_at, updated_at, version)
        VALUES (?, ?, 'Backend Engineer', 'Job Desc', 1, 'MONTHLY', 10000000, 20000000, 'RECRUITING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
        """,
        jobId,
        projectId);

    // Job requirements inserted in specific order
    jdbcTemplate.update(
        "INSERT INTO job_requirement (job_id, position, text) VALUES (?, 0, 'Yêu cầu 1')", jobId);
    jdbcTemplate.update(
        "INSERT INTO job_requirement (job_id, position, text) VALUES (?, 1, 'Yêu cầu 2')", jobId);

    mockMvc
        .perform(get(RouteConstants.PROJECTS + "/" + projectId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.employer").exists())
        .andExpect(jsonPath("$.data.hoursPerDay").value(nullValue()))
        .andExpect(jsonPath("$.data.location").value(nullValue()))
        .andExpect(jsonPath("$.data.duration").value(nullValue()))
        .andExpect(jsonPath("$.data.tags", empty()))
        .andExpect(jsonPath("$.data.jobs[0].requirements", contains("Yêu cầu 1", "Yêu cầu 2")));
  }
}
