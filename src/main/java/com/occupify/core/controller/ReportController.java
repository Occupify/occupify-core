package com.occupify.core.controller;

import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.CreatedResponse;
import com.occupify.core.dto.request.report.CreateReportRequest;
import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.security.Authenticated;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(RouteConstants.REPORTS)
@Tag(name = "Reports", description = "Endpoints for reporting violations, users, and projects")
public class ReportController extends AbstractBaseController {

  private final ReportService reportService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Submit a violation report",
      description = "Submits a new report against a user or project with optional evidence files.")
  public ResponseEntity<CreatedResponse<ReportResponse>> createReport(
      @RequestPart("data") @Valid CreateReportRequest request,
      @RequestPart(value = "files", required = false) List<MultipartFile> files,
      @Parameter(hidden = true) @Authenticated UserContext userContext) {
    log.info(
        "[ReportController] Received submit report request from user [{}] for targetType [{}]",
        userContext.getUserId(),
        request.targetType());
    ReportResponse response = reportService.createReport(request, files, userContext);
    return created(response, MessageConstants.REPORT_CREATED);
  }
}
