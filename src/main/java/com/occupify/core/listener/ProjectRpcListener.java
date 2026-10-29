package com.occupify.core.listener;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.dto.request.ProjectGetRequest;
import com.occupify.core.dto.request.ProjectSearchRequest;
import com.occupify.core.dto.response.agent.AgentProjectResponse;
import com.occupify.core.service.ProjectService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectRpcListener {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("createdAt", "updatedAt", "title", "startPrice", "dueDate");

  private final ProjectService projectService;
  private final RpcResponseFactory responseFactory;

  @RabbitListener(
      queues = "${app.rabbitmq.project-search-queue:occupify.core.rpc.project.search}",
      errorHandler = "agentRpcErrorHandler")
  public Object handleSearchProjects(
      ProjectSearchRequest request,
      @Header(name = "x-trace-id", required = false) String traceId,
      @Header(name = "x-run-id", required = false) String runId,
      @Header(name = "x-tool-call-id", required = false) String toolCallId) {
    if (traceId != null && !traceId.isBlank()) {
      MDC.put("traceId", traceId);
    }
    if (runId != null && !runId.isBlank()) {
      MDC.put("runId", runId);
    }
    if (toolCallId != null && !toolCallId.isBlank()) {
      MDC.put("toolCallId", toolCallId);
    }

    try {
      if (request == null) {
        request =
            new ProjectSearchRequest(
                null, null, null, null, null, null, null, null, 0, 20, "createdAt,desc");
      }

      if (request.minPrice() != null
          && request.maxPrice() != null
          && request.minPrice().compareTo(request.maxPrice()) > 0) {
        throw new IllegalArgumentException("INVALID_PRICE_RANGE");
      }

      Sort sort = parseAndValidateSort(request.sort());

      log.info(
          "[Agent RPC][traceId={}][toolCallId={}] Received search projects request: {}",
          traceId,
          toolCallId,
          request);
      int page = (request.page() != null && request.page() >= 0) ? request.page() : 0;
      int size = (request.size() != null && request.size() > 0) ? Math.min(request.size(), 50) : 20;

      Pageable pageable = PageRequest.of(page, size, sort);
      Page<AgentProjectResponse> result =
          projectService.getAgentProjects(request.toFilter(), pageable);

      log.info(
          "[Agent RPC][traceId={}][toolCallId={}] Search returned {} projects (page {}/{})",
          traceId,
          toolCallId,
          result.getNumberOfElements(),
          result.getNumber(),
          result.getTotalPages());

      return responseFactory.page(
          MessageConstants.PROJECT_LIST_RETRIEVED,
          result.getContent(),
          result.getNumber(),
          result.getSize(),
          result.getTotalElements(),
          result.getTotalPages());
    } finally {
      MDC.remove("traceId");
      MDC.remove("runId");
      MDC.remove("toolCallId");
    }
  }

  @RabbitListener(
      queues = "${app.rabbitmq.project-get-queue:occupify.core.rpc.project.get}",
      errorHandler = "agentRpcErrorHandler")
  public Object handleGetProject(
      ProjectGetRequest request,
      @Header(name = "x-trace-id", required = false) String traceId,
      @Header(name = "x-run-id", required = false) String runId,
      @Header(name = "x-tool-call-id", required = false) String toolCallId) {
    if (traceId != null && !traceId.isBlank()) {
      MDC.put("traceId", traceId);
    }
    if (runId != null && !runId.isBlank()) {
      MDC.put("runId", runId);
    }
    if (toolCallId != null && !toolCallId.isBlank()) {
      MDC.put("toolCallId", toolCallId);
    }

    try {
      if (request == null || request.projectId() == null) {
        throw new IllegalArgumentException("projectId is required");
      }

      log.info(
          "[Agent RPC][traceId={}][toolCallId={}] Received get project request for ID: {}",
          traceId,
          toolCallId,
          request.projectId());
      AgentProjectResponse detail = projectService.getAgentProject(request.projectId());
      return responseFactory.single(detail, MessageConstants.PROJECT_RETRIEVED);
    } finally {
      MDC.remove("traceId");
      MDC.remove("runId");
      MDC.remove("toolCallId");
    }
  }

  private Sort parseAndValidateSort(String sortStr) {
    if (sortStr == null || sortStr.isBlank()) {
      return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    String[] parts = sortStr.split(",");
    String property = parts[0].trim();
    if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
      throw new IllegalArgumentException("Invalid sort property: " + property);
    }
    Sort.Direction direction =
        (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    return Sort.by(direction, property);
  }
}
