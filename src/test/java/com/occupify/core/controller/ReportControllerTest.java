package com.occupify.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.request.report.CreateReportRequest;
import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.enums.ReportReasonCategory;
import com.occupify.core.enums.ReportStatus;
import com.occupify.core.enums.ReportTargetType;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.AuthenticatedUserArgumentResolver;
import com.occupify.core.service.ReportService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import({ResponseFactory.class, AuthenticatedUserArgumentResolver.class})
class ReportControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ReportService reportService;
  @MockBean private UserRepository userRepository;

  @Test
  @DisplayName("POST /reports should submit report with multipart data and return 201 Created")
  void createReport_Success() throws Exception {
    UUID targetUserId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    UUID reporterId = UUID.randomUUID();

    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .reasonCategory(ReportReasonCategory.SPAM)
            .description("Repeated spam in chat and proposals")
            .build();

    MockMultipartFile dataPart =
        new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    MockMultipartFile filePart =
        new MockMultipartFile(
            "files", "screenshot.png", MediaType.IMAGE_PNG_VALUE, "dummy content".getBytes());

    ReportResponse response =
        ReportResponse.builder()
            .id(reportId)
            .reporterId(reporterId)
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .reasonCategory(ReportReasonCategory.SPAM)
            .description("Repeated spam in chat and proposals")
            .status(ReportStatus.PENDING)
            .evidenceUrls(List.of("https://storage.occupify.local/reports/screenshot.png"))
            .createdAt(LocalDateTime.now())
            .build();

    when(reportService.createReport(any(CreateReportRequest.class), any(), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            multipart(RouteConstants.REPORTS)
                .file(dataPart)
                .file(filePart)
                .header("X-User-Id", reporterId.toString())
                .header("X-User-Role", "USER")
                .header("X-User-Email", "reporter@occupify.local")
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value(MessageConstants.REPORT_CREATED))
        .andExpect(jsonPath("$.data.id").value(reportId.toString()))
        .andExpect(jsonPath("$.data.targetType").value("USER"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(
            jsonPath("$.data.evidenceUrls[0]")
                .value("https://storage.occupify.local/reports/screenshot.png"));
  }

  @Test
  @DisplayName("POST /reports with Project and User target context returns 201 Created")
  void createReport_WithProjectAndUserTarget_Success() throws Exception {
    UUID targetUserId = UUID.randomUUID();
    UUID targetProjectId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    UUID reporterId = UUID.randomUUID();

    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .targetProjectId(targetProjectId)
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("User requested outside payment for project deliverable")
            .build();

    MockMultipartFile dataPart =
        new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    MockMultipartFile filePart =
        new MockMultipartFile(
            "files", "evidence.png", MediaType.IMAGE_PNG_VALUE, "fake image bytes".getBytes());

    ReportResponse response =
        ReportResponse.builder()
            .id(reportId)
            .reporterId(reporterId)
            .targetType(ReportTargetType.USER)
            .targetUserId(targetUserId)
            .targetProjectId(targetProjectId)
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("User requested outside payment for project deliverable")
            .status(ReportStatus.PENDING)
            .evidenceUrls(List.of("https://storage.occupify.local/reports/evidence.png"))
            .createdAt(LocalDateTime.now())
            .build();

    when(reportService.createReport(any(CreateReportRequest.class), any(), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            multipart(RouteConstants.REPORTS)
                .file(dataPart)
                .file(filePart)
                .header("X-User-Id", reporterId.toString())
                .header("X-User-Role", "EMPLOYER")
                .header("X-User-Email", "employer@occupify.local")
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.targetProjectId").value(targetProjectId.toString()))
        .andExpect(jsonPath("$.data.reasonCategory").value("FRAUD"));
  }

  @Test
  @DisplayName("POST /reports without required description returns 400 Bad Request")
  void createReport_MissingDescription_Returns400() throws Exception {
    CreateReportRequest request =
        CreateReportRequest.builder()
            .targetType(ReportTargetType.USER)
            .targetUserId(UUID.randomUUID())
            .reasonCategory(ReportReasonCategory.FRAUD)
            .description("") // Blank description violates validation
            .build();

    MockMultipartFile dataPart =
        new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

    mockMvc
        .perform(
            multipart(RouteConstants.REPORTS)
                .file(dataPart)
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "USER")
                .header("X-User-Email", "user@occupify.local")
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }
}
