package com.occupify.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.dto.request.RejectContractRequest;
import com.occupify.core.dto.response.ContractResponse;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.SalaryType;
import com.occupify.core.exception.GlobalExceptionHandler;
import com.occupify.core.security.AuthenticatedUserArgumentResolver;
import com.occupify.core.service.ContractService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContractController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ResponseFactory.class, GlobalExceptionHandler.class})
class ContractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ContractService contractService;
  @MockBean private AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

  private UUID contractId;
  private UUID freelancerId;
  private ContractResponse contractResponse;

  @BeforeEach
  void setUp() {
    contractId = UUID.randomUUID();
    freelancerId = UUID.randomUUID();

    contractResponse =
        new ContractResponse(
            contractId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            freelancerId,
            "Senior Backend Engineer",
            "Contract description",
            BigDecimal.valueOf(3000.00),
            SalaryType.MONTHLY,
            LocalDate.now(),
            LocalDate.now().plusMonths(3),
            "https://storage.occupify.work/contracts/c1.pdf",
            ContractStatus.IN_PROGRESS,
            LocalDateTime.now(),
            LocalDateTime.now());
  }

  @Test
  @DisplayName("PUT /contracts/{id}/accept - Returns 202 ACCEPTED")
  void testAcceptContract_Returns202() throws Exception {
    ContractResponse pendingEscrowResponse =
        new ContractResponse(
            contractId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            freelancerId,
            "Senior Backend Engineer",
            "Contract description",
            BigDecimal.valueOf(3000.00),
            SalaryType.MONTHLY,
            null,
            LocalDate.now().plusMonths(3),
            "https://storage.occupify.work/contracts/c1.pdf",
            ContractStatus.PENDING_ESCROW,
            LocalDateTime.now(),
            LocalDateTime.now());
    when(contractService.acceptContract(eq(contractId), any())).thenReturn(pendingEscrowResponse);

    mockMvc
        .perform(
            put("/contracts/{id}/accept", contractId)
                .header("X-User-Id", freelancerId.toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.statusCode").value(202))
        .andExpect(jsonPath("$.message").value("Contract accepted successfully"))
        .andExpect(jsonPath("$.data.id").value(contractId.toString()))
        .andExpect(jsonPath("$.data.status").value("PENDING_ESCROW"));
  }

  @Test
  @DisplayName("PUT /contracts/{id}/reject - Returns 200 OK")
  void testRejectContract_Returns200() throws Exception {
    ContractResponse rejectedResponse =
        new ContractResponse(
            contractId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            freelancerId,
            "Senior Backend Engineer",
            "Contract description",
            BigDecimal.valueOf(3000.00),
            SalaryType.MONTHLY,
            null,
            null,
            null,
            ContractStatus.CANCELLED,
            LocalDateTime.now(),
            LocalDateTime.now());

    when(contractService.rejectContract(eq(contractId), any(), any())).thenReturn(rejectedResponse);

    RejectContractRequest request = new RejectContractRequest("Not interested");

    mockMvc
        .perform(
            put("/contracts/{id}/reject", contractId)
                .header("X-User-Id", freelancerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.message").value("Contract rejected successfully"))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  @DisplayName("GET /contracts/{id} - Returns 200 OK")
  void testGetContract_Returns200() throws Exception {
    when(contractService.getContract(eq(contractId), any())).thenReturn(contractResponse);

    mockMvc
        .perform(get("/contracts/{id}", contractId).header("X-User-Id", freelancerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value(200))
        .andExpect(jsonPath("$.message").value("Contract retrieved successfully"))
        .andExpect(jsonPath("$.data.id").value(contractId.toString()));
  }
}
