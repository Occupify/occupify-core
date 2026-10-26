package com.occupify.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.constant.MessageConstants;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.request.contract.CancelContractRequest;
import com.occupify.core.dto.request.contract.CompleteContractRequest;
import com.occupify.core.dto.request.contract.CreateContractRequest;
import com.occupify.core.dto.request.contract.RejectContractRequest;
import com.occupify.core.dto.request.contract.UpdateContractRequest;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.security.AuthenticatedUserArgumentResolver;
import com.occupify.core.service.ContractService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(ContractController.class)
@Import({ResponseFactory.class, AuthenticatedUserArgumentResolver.class})
class ContractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ContractService contractService;
  @MockBean private com.occupify.core.repository.user.UserRepository userRepository;

  private final UUID userId = UUID.randomUUID();

  @Test
  @DisplayName("POST /contracts should create contract with multipart data")
  void shouldCreateContract() throws Exception {
    CreateContractRequest request =
        CreateContractRequest.builder()
            .jobId(UUID.randomUUID())
            .title("Hợp đồng Backend Dev")
            .agreedRate(new BigDecimal("35000000.00"))
            .salaryType("MONTHLY")
            .build();

    MockMultipartFile dataPart =
        new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    MockMultipartFile filePart =
        new MockMultipartFile(
            "file", "terms.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes());

    ContractDetailResponse response =
        ContractDetailResponse.builder()
            .id(UUID.randomUUID())
            .title("Hợp đồng Backend Dev")
            .termsFileUrl("https://signed.url/terms.pdf")
            .status(ContractStatus.PENDING)
            .build();

    when(contractService.createContract(any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            multipart(RouteConstants.CONTRACTS)
                .file(dataPart)
                .file(filePart)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_CREATED))
        .andExpect(jsonPath("$.data.title").value("Hợp đồng Backend Dev"))
        .andExpect(jsonPath("$.data.termsFileUrl").value("https://signed.url/terms.pdf"));
  }

  @Test
  @DisplayName("PUT /contracts/{id} should update contract with multipart data")
  void shouldUpdateContract() throws Exception {
    UUID contractId = UUID.randomUUID();
    UpdateContractRequest request =
        UpdateContractRequest.builder()
            .title("Updated Contract Title")
            .agreedRate(new BigDecimal("40000000.00"))
            .salaryType("MONTHLY")
            .build();

    MockMultipartFile dataPart =
        new MockMultipartFile(
            "data", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    MockMultipartFile filePart =
        new MockMultipartFile(
            "file", "terms-v2.pdf", MediaType.APPLICATION_PDF_VALUE, "updated terms".getBytes());

    ContractDetailResponse response =
        ContractDetailResponse.builder()
            .id(contractId)
            .title("Updated Contract Title")
            .termsFileUrl("https://signed.url/terms-v2.pdf")
            .status(ContractStatus.PENDING)
            .build();

    when(contractService.updateContract(eq(contractId), any(), any(), any())).thenReturn(response);

    MockMultipartHttpServletRequestBuilder builder =
        multipart(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_ID, contractId);
    builder.with(
        req -> {
          req.setMethod(HttpMethod.PUT.name());
          return req;
        });

    mockMvc
        .perform(
            builder
                .file(dataPart)
                .file(filePart)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_UPDATED))
        .andExpect(jsonPath("$.data.title").value("Updated Contract Title"));
  }

  @Test
  @DisplayName("GET /contracts should return paginated list")
  void shouldGetContractsPaginated() throws Exception {
    ContractSummaryResponse summary =
        ContractSummaryResponse.builder()
            .id(UUID.randomUUID())
            .title("Contract 1")
            .status(ContractStatus.PENDING)
            .build();

    when(contractService.getContracts(any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(
            get(RouteConstants.CONTRACTS)
                .param("page", "0")
                .param("size", "20")
                .param("status", "PENDING")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_LIST_RETRIEVED))
        .andExpect(jsonPath("$.data[0].title").value("Contract 1"))
        .andExpect(jsonPath("$.data[0].status").value("PENDING"));
  }

  @Test
  @DisplayName("GET /contracts/{id} should return contract detail")
  void shouldGetContractDetail() throws Exception {
    UUID contractId = UUID.randomUUID();
    ContractDetailResponse response =
        ContractDetailResponse.builder()
            .id(contractId)
            .title("Detail Title")
            .status(ContractStatus.IN_PROGRESS)
            .build();

    when(contractService.getContractDetail(eq(contractId), any())).thenReturn(response);

    mockMvc
        .perform(
            get(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_ID, contractId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_RETRIEVED))
        .andExpect(jsonPath("$.data.id").value(contractId.toString()))
        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
  }

  @Test
  @DisplayName("POST /contracts/{id}/cancel with JSON body should cancel contract")
  void shouldCancelContractWithJsonBody() throws Exception {
    UUID contractId = UUID.randomUUID();
    CancelContractRequest cancelReq = new CancelContractRequest("Lý do hủy hợp đồng đã đủ dài");

    ContractDetailResponse response =
        ContractDetailResponse.builder().id(contractId).status(ContractStatus.CANCELLED).build();

    when(contractService.cancelContract(eq(contractId), any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_CANCEL, contractId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReq))
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_CANCELLED))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("POST /contracts/{id}/cancel with multipart data and file should cancel contract")
  void shouldCancelContractWithMultipart() throws Exception {
    UUID contractId = UUID.randomUUID();
    CancelContractRequest cancelReq =
        new CancelContractRequest("Lý do hủy hợp đồng với file đính kèm");

    MockMultipartFile dataPart =
        new MockMultipartFile(
            "data",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(cancelReq));
    MockMultipartFile filePart =
        new MockMultipartFile(
            "file", "evidence.pdf", MediaType.APPLICATION_PDF_VALUE, "evidence proof".getBytes());

    ContractDetailResponse response =
        ContractDetailResponse.builder().id(contractId).status(ContractStatus.CANCELLED).build();

    when(contractService.cancelContract(eq(contractId), any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            multipart(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_CANCEL, contractId)
                .file(dataPart)
                .file(filePart)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_CANCELLED))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("POST /contracts/{id}/accept should accept contract and return 202 ACCEPTED")
  void shouldAcceptContract() throws Exception {
    UUID contractId = UUID.randomUUID();
    ContractDetailResponse response =
        ContractDetailResponse.builder()
            .id(contractId)
            .status(ContractStatus.PENDING_ESCROW)
            .build();

    when(contractService.acceptContract(eq(contractId), any())).thenReturn(response);

    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_ACCEPT, contractId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.statusCode").value(202))
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_ACCEPTED))
        .andExpect(jsonPath("$.data.status").value("PENDING_ESCROW"));
  }

  @Test
  @DisplayName("PUT /contracts/{id}/accept should accept contract and return 202 ACCEPTED")
  void shouldAcceptContractWithPut() throws Exception {
    UUID contractId = UUID.randomUUID();
    ContractDetailResponse response =
        ContractDetailResponse.builder()
            .id(contractId)
            .status(ContractStatus.PENDING_ESCROW)
            .build();

    when(contractService.acceptContract(eq(contractId), any())).thenReturn(response);

    mockMvc
        .perform(
            put(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_ACCEPT, contractId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.statusCode").value(202))
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_ACCEPTED))
        .andExpect(jsonPath("$.data.status").value("PENDING_ESCROW"));
  }

  @Test
  @DisplayName("POST /contracts/{id}/reject should reject contract")
  void shouldRejectContract() throws Exception {
    UUID contractId = UUID.randomUUID();
    RejectContractRequest rejectReq =
        RejectContractRequest.builder().reason("Mức thù lao chưa phù hợp").build();

    ContractDetailResponse response =
        ContractDetailResponse.builder().id(contractId).status(ContractStatus.CANCELLED).build();

    when(contractService.rejectContract(eq(contractId), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_REJECT, contractId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectReq))
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_REJECTED))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("PUT /contracts/{id}/reject should reject contract")
  void shouldRejectContractWithPut() throws Exception {
    UUID contractId = UUID.randomUUID();
    RejectContractRequest rejectReq =
        RejectContractRequest.builder().reason("Mức thù lao chưa phù hợp").build();

    ContractDetailResponse response =
        ContractDetailResponse.builder().id(contractId).status(ContractStatus.CANCELLED).build();

    when(contractService.rejectContract(eq(contractId), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            put(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_REJECT, contractId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectReq))
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "USER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_REJECTED))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("POST /contracts/{id}/complete should complete contract")
  void shouldCompleteContract() throws Exception {
    UUID contractId = UUID.randomUUID();
    CompleteContractRequest completeReq =
        CompleteContractRequest.builder().rating(5).comment("Excellent work!").build();

    ContractDetailResponse response =
        ContractDetailResponse.builder().id(contractId).status(ContractStatus.COMPLETED).build();

    when(contractService.completeContract(eq(contractId), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            post(RouteConstants.CONTRACTS + RouteConstants.CONTRACT_COMPLETE, contractId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeReq))
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "EMPLOYER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(MessageConstants.CONTRACT_COMPLETED))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));
  }
}
