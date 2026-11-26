package com.occupify.core.listener;

import com.occupify.core.dto.request.job.CreateJobRequest;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.enums.UserRole;
import com.occupify.core.event.AgentCreateProjectAndJobsEvent;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.JobService;
import com.occupify.core.service.ProjectService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentProjectCreateListener {

  private final ProjectService projectService;
  private final JobService jobService;

  @RabbitListener(
      queues = "${app.rabbitmq.agent-project-create-queue:occupify.core.agent.project.create}")
  @Transactional
  public void handleCreateProjectAndJobs(AgentCreateProjectAndJobsEvent event) {
    if (event == null || event.userId() == null || event.project() == null) {
      log.warn("[Agent Project Consumer] Received invalid AgentCreateProjectAndJobsEvent: {}", event);
      return;
    }

    String email = event.userEmail() != null ? event.userEmail() : "";
    UserContext userContext = new UserContext(event.userId(), UserRole.USER, email);

    log.info(
        "[Agent Project Consumer][traceId={}] Creating project '{}' for user [{}]",
        event.traceId(),
        event.project().title(),
        event.userId());

    ProjectResponse createdProject = projectService.createProject(event.project(), userContext);

    List<CreateJobRequest> jobs = event.jobs();
    int createdJobs = 0;
    if (jobs != null) {
      for (CreateJobRequest jobReq : jobs) {
        if (jobReq == null) {
          continue;
        }
        CreateJobRequest boundJobReq =
            CreateJobRequest.builder()
                .projectId(createdProject.id())
                .role(jobReq.role())
                .title(jobReq.title())
                .description(jobReq.description())
                .budget(jobReq.budget())
                .salaryType(jobReq.salaryType())
                .startedAt(jobReq.startedAt())
                .finishedAt(jobReq.finishedAt())
                .skillIds(jobReq.skillIds())
                .build();
        jobService.createJob(boundJobReq, null, userContext);
        createdJobs++;
      }
    }

    log.info(
        "[Agent Project Consumer][traceId={}] Created project [{}] with {} job(s)",
        event.traceId(),
        createdProject.id(),
        createdJobs);
  }
}
