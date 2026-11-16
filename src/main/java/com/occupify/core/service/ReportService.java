package com.occupify.core.service;

import com.occupify.core.dto.request.report.CreateReportRequest;
import com.occupify.core.dto.response.report.ReportResponse;
import com.occupify.core.security.UserContext;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ReportService {

  ReportResponse createReport(
      CreateReportRequest request, List<MultipartFile> files, UserContext userContext);
}
