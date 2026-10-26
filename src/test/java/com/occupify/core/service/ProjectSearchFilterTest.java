package com.occupify.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.enums.UserStatus;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.project.ProjectRepository;
import com.occupify.core.repository.skill.SkillRepository;
import com.occupify.core.repository.user.UserRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectSearchFilterTest {

  @Autowired private ProjectService projectService;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private JobRepository jobRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SkillRepository skillRepository;

  private Project project;

  @BeforeEach
  void setUp() {
    User employer =
        userRepository.save(
            User.builder()
                .email("owner@occupify.local")
                .fullName("Chủ dự án mẫu")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    Skill skillA = skillRepository.save(Skill.builder().name("Next.js").build());
    Skill skillB = skillRepository.save(Skill.builder().name("Docker").build());

    project =
        projectRepository.save(
            Project.builder()
                .employer(employer)
                .title("Dự án Microservices Cloud")
                .description("Phát triển dịch vụ cloud")
                .startPrice(BigDecimal.valueOf(15000000))
                .period(ProjectPeriod.MONTHLY)
                .status(ProjectStatus.OPEN)
                .build());

    jobRepository.save(
        Job.builder()
            .project(project)
            .title("Frontend Developer")
            .budgetMin(BigDecimal.valueOf(18000000))
            .budgetMax(BigDecimal.valueOf(25000000))
            .status(JobStatus.RECRUITING)
            .skills(Set.of(skillA))
            .build());

    jobRepository.save(
        Job.builder()
            .project(project)
            .title("DevOps Engineer")
            .budgetMin(BigDecimal.valueOf(22000000))
            .budgetMax(BigDecimal.valueOf(35000000))
            .status(JobStatus.RECRUITING)
            .skills(Set.of(skillB))
            .build());
  }

  @Test
  void testGetProjects_enrichesCardFields() {
    ProjectFilterRequest request =
        new ProjectFilterRequest("Microservices", null, null, null, null, null, null, null);
    Page<ProjectResponse> page = projectService.getProjects(request, PageRequest.of(0, 10));

    assertNotNull(page);
    assertFalse(page.isEmpty());

    ProjectResponse card = page.getContent().get(0);
    assertEquals(project.getId(), card.id());
    assertEquals(2, card.openJobsCount());
    assertEquals(2, card.jobs().size());
    assertEquals(project.getId(), card.jobs().get(0).projectId());
    assertEquals(0, card.jobs().get(0).bidCount());
    assertTrue(card.skills().contains("Next.js"));
    assertTrue(card.skills().contains("Docker"));
  }

  private void assertFalse(boolean condition) {
    org.junit.jupiter.api.Assertions.assertFalse(condition);
  }
}
