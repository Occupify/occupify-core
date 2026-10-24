package com.occupify.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.occupify.core.dto.request.ProjectFilterRequest;
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
import com.occupify.core.repository.project.ProjectSpecification;
import com.occupify.core.repository.skill.SkillRepository;
import com.occupify.core.repository.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectSpecificationTest {

  @Autowired private ProjectRepository projectRepository;
  @Autowired private JobRepository jobRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SkillRepository skillRepository;

  private User employer;
  private Skill skillJava;
  private Skill skillReact;
  private Skill skillFigma;

  private Project activeProject;
  private Project closedProject;
  private Project projectNoRecruitingJobs;

  @BeforeEach
  void setUp() {
    employer =
        userRepository.save(
            User.builder()
                .email("employer.test@occupify.local")
                .fullName("Nguyễn Văn Chủ")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    skillJava = skillRepository.save(Skill.builder().name("Java").build());
    skillReact = skillRepository.save(Skill.builder().name("React").build());
    skillFigma = skillRepository.save(Skill.builder().name("Figma").build());

    // 1. Active project with 2 recruiting jobs
    activeProject =
        projectRepository.save(
            Project.builder()
                .employer(employer)
                .title("Xây dựng sàn Thương mại điện tử")
                .description("Phát triển backend Java và giao diện React cho sàn TMĐT")
                .startPrice(BigDecimal.valueOf(20000000))
                .period(ProjectPeriod.MONTHLY)
                .status(ProjectStatus.OPEN)
                .build());

    Job job1 =
        jobRepository.save(
            Job.builder()
                .project(activeProject)
                .title("Lập trình viên Backend Senior")
                .description("Thiết kế API hệ thống bằng Spring Boot")
                .budgetMin(BigDecimal.valueOf(20000000))
                .budgetMax(BigDecimal.valueOf(35000000))
                .status(JobStatus.RECRUITING)
                .skills(Set.of(skillJava))
                .build());

    Job job2 =
        jobRepository.save(
            Job.builder()
                .project(activeProject)
                .title("Kỹ sư Frontend")
                .description("Xây dựng UI responsive bằng React và Tailwind")
                .budgetMin(BigDecimal.valueOf(15000000))
                .budgetMax(BigDecimal.valueOf(22000000))
                .status(JobStatus.RECRUITING)
                .skills(Set.of(skillReact))
                .build());

    // 2. Closed project
    closedProject =
        projectRepository.save(
            Project.builder()
                .employer(employer)
                .title("Dự án đã kết thúc")
                .description("Thiết kế giao diện Figma cho ứng dụng tài chính")
                .startPrice(BigDecimal.valueOf(10000000))
                .period(ProjectPeriod.FIXED)
                .status(ProjectStatus.CLOSED)
                .build());

    jobRepository.save(
        Job.builder()
            .project(closedProject)
            .title("UI/UX Designer")
            .budgetMin(BigDecimal.valueOf(10000000))
            .budgetMax(BigDecimal.valueOf(15000000))
            .status(JobStatus.CLOSED)
            .skills(Set.of(skillFigma))
            .build());

    // 3. Open project but all jobs filled
    projectNoRecruitingJobs =
        projectRepository.save(
            Project.builder()
                .employer(employer)
                .title("Dự án đã đủ người")
                .description("Đã tuyển đủ lập trình viên")
                .startPrice(BigDecimal.valueOf(12000000))
                .period(ProjectPeriod.MONTHLY)
                .status(ProjectStatus.OPEN)
                .build());

    jobRepository.save(
        Job.builder()
            .project(projectNoRecruitingJobs)
            .title("Fresher Java")
            .budgetMin(BigDecimal.valueOf(10000000))
            .budgetMax(BigDecimal.valueOf(12000000))
            .status(JobStatus.FILLED)
            .skills(Set.of(skillJava))
            .build());
  }

  @Test
  void testDefaultFilter_excludesClosedAndNonRecruitingProjects() {
    Specification<Project> spec = ProjectSpecification.withFilters(null);
    Page<Project> page = projectRepository.findAll(spec, PageRequest.of(0, 10));

    assertTrue(page.getContent().stream().anyMatch(p -> p.getId().equals(activeProject.getId())));
    assertFalse(page.getContent().stream().anyMatch(p -> p.getId().equals(closedProject.getId())));
    assertFalse(
        page.getContent().stream()
            .anyMatch(p -> p.getId().equals(projectNoRecruitingJobs.getId())));
  }

  @Test
  void testSearchKeyword_withUnaccentedVietnamese() {
    // "thuong mai dien tu" unaccented matches "Xây dựng sàn Thương mại điện tử"
    ProjectFilterRequest request =
        new ProjectFilterRequest("thuong mai dien tu", null, null, null, null, null, null, null);
    Page<Project> page =
        projectRepository.findAll(ProjectSpecification.withFilters(request), PageRequest.of(0, 10));

    assertEquals(1, page.getTotalElements());
    assertEquals(activeProject.getId(), page.getContent().get(0).getId());
  }

  @Test
  void testSearchKeyword_matchingJobTitleAndSkill() {
    // "lap trinh vien" unaccented matches job title "Lập trình viên Backend Senior"
    ProjectFilterRequest request =
        new ProjectFilterRequest("lap trinh vien", null, null, null, null, null, null, null);
    Page<Project> page =
        projectRepository.findAll(ProjectSpecification.withFilters(request), PageRequest.of(0, 10));

    assertEquals(1, page.getTotalElements());
    assertEquals(activeProject.getId(), page.getContent().get(0).getId());
  }

  @Test
  void testSkillsFilter_orLogic() {
    // Requesting skills [Figma, React] should match activeProject because it has React
    ProjectFilterRequest request =
        new ProjectFilterRequest(
            null, null, null, null, null, null, null, List.of("Figma", "React"));
    Page<Project> page =
        projectRepository.findAll(ProjectSpecification.withFilters(request), PageRequest.of(0, 10));

    assertEquals(1, page.getTotalElements());
    assertEquals(activeProject.getId(), page.getContent().get(0).getId());
  }

  @Test
  void testBudgetFilter_overlappingRange() {
    // Filter [18M, 25M] overlaps with job1 [20M, 35M] and job2 [15M, 22M]
    ProjectFilterRequest request =
        new ProjectFilterRequest(
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(18000000),
            BigDecimal.valueOf(25000000),
            null);
    Page<Project> page =
        projectRepository.findAll(ProjectSpecification.withFilters(request), PageRequest.of(0, 10));

    assertEquals(1, page.getTotalElements());
    assertEquals(activeProject.getId(), page.getContent().get(0).getId());

    // Filter [50M, 60M] does NOT overlap
    ProjectFilterRequest requestNoMatch =
        new ProjectFilterRequest(
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(50000000),
            BigDecimal.valueOf(60000000),
            null);
    Page<Project> emptyPage =
        projectRepository.findAll(
            ProjectSpecification.withFilters(requestNoMatch), PageRequest.of(0, 10));

    assertEquals(0, emptyPage.getTotalElements());
  }

  @Test
  void testBackwardCompatibility_employerCanQueryClosedProjects() {
    // Explicitly querying by employerId and status CLOSED
    ProjectFilterRequest request =
        new ProjectFilterRequest(
            null, ProjectStatus.CLOSED, employer.getId(), null, null, null, null, null);
    Page<Project> page =
        projectRepository.findAll(ProjectSpecification.withFilters(request), PageRequest.of(0, 10));

    assertEquals(1, page.getTotalElements());
    assertEquals(closedProject.getId(), page.getContent().get(0).getId());
  }
}
