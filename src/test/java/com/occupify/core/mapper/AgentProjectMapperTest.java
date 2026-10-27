package com.occupify.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.dto.response.agent.AgentJobResponse;
import com.occupify.core.dto.response.agent.AgentProjectResponse;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.skill.Skill;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AgentProjectMapperTest {

  private final AgentProjectMapper mapper = new AgentProjectMapper();

  @Test
  void testSortingAndUniqueness() {
    UUID projectId = UUID.randomUUID();
    Project project =
        Project.builder()
            .title("Project Title")
            .period(ProjectPeriod.MONTHLY)
            .status(ProjectStatus.OPEN)
            .tags(Set.of("mobile app", "Backend", "AI", "frontend"))
            .version(1L)
            .build();
    project.setId(projectId);

    Skill sJava = Skill.builder().name("Java").build();
    Skill sPython = Skill.builder().name("python").build();
    Skill sAws = Skill.builder().name("AWS").build();

    UUID j1Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID j2Id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    LocalDateTime now = LocalDateTime.now();
    Job j1 =
        Job.builder()
            .project(project)
            .title("Job 1")
            .status(JobStatus.RECRUITING)
            .skills(Set.of(sPython, sJava))
            .requirements(List.of("Req B", "Req A", "Req C"))
            .version(0L)
            .build();
    j1.setId(j1Id);
    j1.setCreatedAt(now.minusHours(1));

    Job j2 =
        Job.builder()
            .project(project)
            .title("Job 2")
            .status(JobStatus.FILLED)
            .skills(Set.of(sAws, sJava))
            .requirements(List.of("Req 1"))
            .version(0L)
            .build();
    j2.setId(j2Id);
    j2.setCreatedAt(now);

    AgentProjectResponse response = mapper.toResponse(project, List.of(j2, j1), Map.of(j1Id, 5));

    // Tags sorted A->Z case-insensitive
    assertThat(response.tags()).containsExactly("AI", "Backend", "frontend", "mobile app");

    // Union skills sorted A->Z case-insensitive
    assertThat(response.skills()).containsExactly("AWS", "Java", "python");

    // Jobs sorted by createdAt then id: j1 (1h ago) before j2 (now)
    assertThat(response.jobs()).hasSize(2);
    assertThat(response.jobs().get(0).id()).isEqualTo(j1Id);
    assertThat(response.jobs().get(1).id()).isEqualTo(j2Id);

    // Requirements in stored order
    assertThat(response.jobs().get(0).requirements()).containsExactly("Req B", "Req A", "Req C");
    assertThat(response.jobs().get(0).bidCount()).isEqualTo(5);
    assertThat(response.openJobsCount()).isEqualTo(1);
  }

  @Test
  void testNullPolicy() {
    Project sparseProject = Project.builder().title("Sparse").build();
    sparseProject.setId(UUID.randomUUID());

    AgentProjectResponse response = mapper.toResponse(sparseProject, List.of(), Map.of());

    assertThat(response.hoursPerDay()).isNull();
    assertThat(response.location()).isNull();
    assertThat(response.duration()).isNull();
    assertThat(response.closedAt()).isNull();

    // Collections are always non-null []
    assertThat(response.tags()).isNotNull().isEmpty();
    assertThat(response.skills()).isNotNull().isEmpty();
    assertThat(response.jobs()).isNotNull().isEmpty();
  }
}
