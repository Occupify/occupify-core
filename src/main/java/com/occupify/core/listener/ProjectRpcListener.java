package com.occupify.core.listener;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.dto.request.ProjectGetRequest;
import com.occupify.core.dto.request.ProjectSearchRequest;
import com.occupify.core.dto.response.ProjectDetailResponse;
import com.occupify.core.exception.BaseException;
import com.occupify.core.service.ProjectService;
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

  private final ProjectService projectService;
  private final RpcResponseFactory responseFactory;

  @RabbitListener(queues = "${app.rabbitmq.project-search-queue:occupify.core.rpc.project.search}")
  public Object handleSearchProjects(
      ProjectSearchRequest request,
      @Header(name = "x-trace-id", required = false) String traceId,
      @Header(name = "x-run-id", required = false) String runId,
      @Header(name = "x-tool-call-id", required = false) String toolCallId) {
    try {
      if (traceId != null && !traceId.isBlank()) {
        MDC.put("traceId", traceId);
      }
      if (runId != null && !runId.isBlank()) {
        MDC.put("runId", runId);
      }
      if (toolCallId != null && !toolCallId.isBlank()) {
        MDC.put("toolCallId", toolCallId);
      }

      if (request == null) {
        request =
            new ProjectSearchRequest(
                null, null, null, null, null, null, null, null, 0, 20, "createdAt,desc");
      }

      if (request.minPrice() != null
          && request.maxPrice() != null
          && request.minPrice().compareTo(request.maxPrice()) > 0) {
        return responseFactory.error(
            400,
            "Invalid price filter: 'minPrice' ("
                + request.minPrice()
                + ") cannot be greater than 'maxPrice' ("
                + request.maxPrice()
                + ").",
            "INVALID_PRICE_RANGE");
      }

      log.info(
          "[Agent RPC][traceId={}][toolCallId={}] Received search projects request: {}",
          traceId,
          toolCallId,
          request);
      int page = (request.page() != null && request.page() >= 0) ? request.page() : 0;
      int size = (request.size() != null && request.size() > 0) ? Math.min(request.size(), 50) : 20;
      Sort sort = parseSort(request.sort());

      Pageable pageable = PageRequest.of(page, size, sort);
      Page<ProjectDetailResponse> result =
          projectService.getProjectsWithDetails(request.toFilter(), pageable);

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
    } catch (BaseException be) {
      log.warn(
          "[Agent RPC][traceId={}][toolCallId={}] Business error during project search: {}",
          traceId,
          toolCallId,
          be.getMessage());
      return responseFactory.error(be);
    } catch (Exception ex) {
      log.error(
          "[Agent RPC][traceId={}][toolCallId={}] Unexpected error during project search",
          traceId,
          toolCallId,
          ex);
      return responseFactory.error(
          500, "Failed to search projects: " + ex.getMessage(), "INTERNAL_SERVER_ERROR");
    } finally {
      MDC.remove("traceId");
      MDC.remove("runId");
      MDC.remove("toolCallId");
    }
  }

  @RabbitListener(queues = "${app.rabbitmq.project-get-queue:occupify.core.rpc.project.get}")
  public Object handleGetProject(
      ProjectGetRequest request,
      @Header(name = "x-trace-id", required = false) String traceId,
      @Header(name = "x-run-id", required = false) String runId,
      @Header(name = "x-tool-call-id", required = false) String toolCallId) {
    try {
      if (traceId != null && !traceId.isBlank()) {
        MDC.put("traceId", traceId);
      }
      if (runId != null && !runId.isBlank()) {
        MDC.put("runId", runId);
      }
      if (toolCallId != null && !toolCallId.isBlank()) {
        MDC.put("toolCallId", toolCallId);
      }

      if (request == null || request.projectId() == null) {
        return responseFactory.error(
            400,
            "Invalid request: 'projectId' is required and must be a valid UUID.",
            "INVALID_ARGUMENT");
      }

      log.info(
          "[Agent RPC][traceId={}][toolCallId={}] Received get project request for ID: {}",
          traceId,
          toolCallId,
          request.projectId());
      ProjectDetailResponse detail = projectService.getProjectDetail(request.projectId());
      return responseFactory.single(detail, MessageConstants.PROJECT_RETRIEVED);
    } catch (BaseException be) {
      log.warn(
          "[Agent RPC][traceId={}][toolCallId={}] Business error during get project [{}]: {}",
          traceId,
          toolCallId,
          request.projectId(),
          be.getMessage());
      return responseFactory.error(be);
    } catch (Exception ex) {
      log.error(
          "[Agent RPC][traceId={}][toolCallId={}] Unexpected error during get project [{}]",
          traceId,
          toolCallId,
          request != null ? request.projectId() : null,
          ex);
      return responseFactory.error(
          500, "Failed to retrieve project: " + ex.getMessage(), "INTERNAL_SERVER_ERROR");
    } finally {
      MDC.remove("traceId");
      MDC.remove("runId");
      MDC.remove("toolCallId");
    }
  }

  private Sort parseSort(String sortStr) {
    if (sortStr == null || sortStr.isBlank()) {
      return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    String[] parts = sortStr.split(",");
    String property = parts[0].trim();
    Sort.Direction direction =
        (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    return Sort.by(direction, property);
  }
}
