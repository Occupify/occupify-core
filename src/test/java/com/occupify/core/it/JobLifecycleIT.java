package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.repository.user.UserRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public class JobLifecycleIT extends AbstractIntegrationIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;

  private User employer;
  private User freelancer1;
  private User freelancer2;

  @BeforeEach
  void setUp() {
    truncateTables();
    employer =
        userRepository.save(
            User.builder()
                .email("employer.lifecycle@example.com")
                .fullName("Employer Lifecycle")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    freelancer1 =
        userRepository.save(
            User.builder()
                .email("freelancer1.lifecycle@example.com")
                .fullName("Freelancer 1")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    freelancer2 =
        userRepository.save(
            User.builder()
                .email("freelancer2.lifecycle@example.com")
                .fullName("Freelancer 2")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
  }

  @AfterEach
  void tearDown() {
    truncateTables();
  }

  @Test
  void acceptAndCancelOnPostgres() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID contract1Id = UUID.randomUUID();
    UUID contract2Id = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, status, version, created_at, updated_at)
        VALUES (?, ?, 'Lifecycle Project', 'Desc', 10000000, 'MONTHLY', 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        projectId,
        employer.getId());

    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Job Qty 2', 'Desc', 2, 'MONTHLY', 5000000, 10000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        jobId,
        projectId);

    // Insert two PENDING contracts
    jdbcTemplate.update(
        """
        INSERT INTO contracts (id, employer_id, employee_id, job_id, title, agreed_rate, salary_type, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, 'Contract 1', 5000000, 'MONTHLY', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        contract1Id,
        employer.getId(),
        freelancer1.getId(),
        jobId);

    jdbcTemplate.update(
        """
        INSERT INTO contracts (id, employer_id, employee_id, job_id, title, agreed_rate, salary_type, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, 'Contract 2', 5000000, 'MONTHLY', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        contract2Id,
        employer.getId(),
        freelancer2.getId(),
        jobId);

    // Accept contract 1: active = 1 < 2 -> Job remains RECRUITING
    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + "/" + contract1Id + "/accept")
                .header("X-User-Id", freelancer1.getId().toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk());

    String statusAfterFirst =
        jdbcTemplate.queryForObject("SELECT status FROM job WHERE id = ?", String.class, jobId);
    assertThat(statusAfterFirst).isEqualTo(JobStatus.RECRUITING.name());

    // Accept contract 2: active = 2 >= 2 -> Job becomes FILLED
    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + "/" + contract2Id + "/accept")
                .header("X-User-Id", freelancer2.getId().toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk());

    String statusAfterSecond =
        jdbcTemplate.queryForObject("SELECT status FROM job WHERE id = ?", String.class, jobId);
    assertThat(statusAfterSecond).isEqualTo(JobStatus.FILLED.name());

    // Cancel contract 1: active = 1 < 2 -> Job transitions back to RECRUITING
    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + "/" + contract1Id + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER")
                .content("{\"reason\":\"Freelancer unavailable\"}"))
        .andExpect(status().isOk());

    String statusAfterCancel =
        jdbcTemplate.queryForObject("SELECT status FROM job WHERE id = ?", String.class, jobId);
    assertThat(statusAfterCancel).isEqualTo(JobStatus.RECRUITING.name());
  }

  @Test
  void concurrentAcceptsFillOnce() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, status, version, created_at, updated_at)
        VALUES (?, ?, 'Concurrent Project', 'Desc', 10000000, 'MONTHLY', 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        projectId,
        employer.getId());

    // Job quantity 1: only 1 slot open
    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Job Qty 1', 'Desc', 1, 'MONTHLY', 5000000, 10000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        jobId,
        projectId);

    jdbcTemplate.update(
        """
        INSERT INTO contracts (id, employer_id, employee_id, job_id, title, agreed_rate, salary_type, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, 'Contract 1', 5000000, 'MONTHLY', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        c1,
        employer.getId(),
        freelancer1.getId(),
        jobId);

    jdbcTemplate.update(
        """
        INSERT INTO contracts (id, employer_id, employee_id, job_id, title, agreed_rate, salary_type, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, 'Contract 2', 5000000, 'MONTHLY', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        c2,
        employer.getId(),
        freelancer2.getId(),
        jobId);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(2);
    AtomicInteger successCount = new AtomicInteger(0);

    executor.submit(
        () -> {
          try {
            startLatch.await();
            mockMvc.perform(
                post(RouteConstants.CONTRACTS + "/" + c1 + "/accept")
                    .header("X-User-Id", freelancer1.getId().toString())
                    .header("X-User-Role", "USER"));
            successCount.incrementAndGet();
          } catch (Exception ignored) {
          } finally {
            doneLatch.countDown();
          }
        });

    executor.submit(
        () -> {
          try {
            startLatch.await();
            mockMvc.perform(
                post(RouteConstants.CONTRACTS + "/" + c2 + "/accept")
                    .header("X-User-Id", freelancer2.getId().toString())
                    .header("X-User-Role", "USER"));
            successCount.incrementAndGet();
          } catch (Exception ignored) {
          } finally {
            doneLatch.countDown();
          }
        });

    startLatch.countDown();
    boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(finished).isTrue();

    String jobStatus =
        jdbcTemplate.queryForObject("SELECT status FROM job WHERE id = ?", String.class, jobId);
    assertThat(jobStatus).isEqualTo(JobStatus.FILLED.name());
  }

  @Test
  void closeProjectClosesRecruitingJobs() throws Exception {
    UUID projectId = UUID.randomUUID();
    UUID recruitingJobId = UUID.randomUUID();
    UUID filledJobId = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO projects (id, employer_id, title, description, start_price, period, status, version, created_at, updated_at)
        VALUES (?, ?, 'Project to Close', 'Desc', 10000000, 'MONTHLY', 'OPEN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        projectId,
        employer.getId());

    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Recruiting Job', 'Desc', 1, 'MONTHLY', 5000000, 10000000, 'RECRUITING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        recruitingJobId,
        projectId);

    jdbcTemplate.update(
        """
        INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, version, created_at, updated_at)
        VALUES (?, ?, 'Filled Job', 'Desc', 1, 'MONTHLY', 5000000, 10000000, 'FILLED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        filledJobId,
        projectId);

    mockMvc
        .perform(
            patch(RouteConstants.PROJECTS + "/" + projectId + "/close")
                .header("X-User-Id", employer.getId().toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk());

    String recruitingAfterClose =
        jdbcTemplate.queryForObject(
            "SELECT status FROM job WHERE id = ?", String.class, recruitingJobId);
    assertThat(recruitingAfterClose).isEqualTo(JobStatus.CLOSED.name());

    String filledAfterClose =
        jdbcTemplate.queryForObject(
            "SELECT status FROM job WHERE id = ?", String.class, filledJobId);
    assertThat(filledAfterClose).isEqualTo(JobStatus.FILLED.name());
  }
}
