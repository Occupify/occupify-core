package com.occupify.core.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.occupify.core.constant.RouteConstants;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectSearchFilterControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private JobRepository jobRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SkillRepository skillRepository;

  private Project activeProject;

  @BeforeEach
  void setUp() {
    User employer =
        userRepository.save(
            User.builder()
                .email("ctrl.employer@occupify.local")
                .fullName("Nhà Tuyển Dụng MockMvc")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

    Skill skillFigma = skillRepository.save(Skill.builder().name("Figma").build());
    Skill skillVue = skillRepository.save(Skill.builder().name("Vue.js").build());

    activeProject =
        projectRepository.save(
            Project.builder()
                .employer(employer)
                .title("Ứng dụng Quản lý Tài chính Cá nhân")
                .description("Thiết kế giao diện đẹp và trực quan cho app tài chính")
                .startPrice(BigDecimal.valueOf(25000000))
                .period(ProjectPeriod.MONTHLY)
                .status(ProjectStatus.OPEN)
                .build());

    jobRepository.save(
        Job.builder()
            .project(activeProject)
            .title("Lead UI Designer")
            .budgetMin(BigDecimal.valueOf(20000000))
            .budgetMax(BigDecimal.valueOf(30000000))
            .status(JobStatus.RECRUITING)
            .skills(Set.of(skillFigma, skillVue))
            .build());
  }

  @Test
  void testSearchAndFilterEndpoint_withKeywordAndSkills() throws Exception {
    mockMvc
        .perform(
            get(RouteConstants.PROJECTS)
                .param("keyword", "quan ly tai chinh")
                .param("skills", "Figma", "React")
                .param("minPrice", "15000000")
                .param("maxPrice", "35000000")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(activeProject.getId().toString()))
        .andExpect(jsonPath("$.data[0].title").value("Ứng dụng Quản lý Tài chính Cá nhân"))
        .andExpect(jsonPath("$.data[0].salaryMin").value(20000000.0))
        .andExpect(jsonPath("$.data[0].salaryMax").value(30000000.0))
        .andExpect(jsonPath("$.data[0].openJobsCount").value(1))
        .andExpect(jsonPath("$.paging.total").value(1));
  }

  @Test
  void testSearchEndpoint_unaccentedVietnameseMatching() throws Exception {
    mockMvc
        .perform(get(RouteConstants.PROJECTS).param("keyword", "ung dung"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(activeProject.getId().toString()));
  }
}
