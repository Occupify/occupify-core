package com.occupify.core.event;

import com.occupify.core.dto.request.CreateProjectRequest;
import com.occupify.core.dto.request.job.CreateJobRequest;
import java.util.List;
import java.util.UUID;

public record AgentCreateProjectAndJobsEvent(
    UUID eventId,
    UUID userId,
    String userEmail,
    String traceId,
    CreateProjectRequest project,
    List<CreateJobRequest> jobs) {}
