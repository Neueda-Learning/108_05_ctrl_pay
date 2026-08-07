package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.domain.ValidationResultRecord;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentAuditControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private PaymentRecord samplePayment;

    @BeforeEach
    void setUp() {
        PaymentAuditController controller = new PaymentAuditController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        samplePayment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("GET /api/payments/1/audit/status-history: Success status history")
    void getStatusHistory_Success() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(samplePayment));

        PaymentStatusHistoryRecord historyRecord = PaymentStatusHistoryRecord.transition(
            1L, PaymentStatus.CREATED, PaymentStatus.VALIDATED, "SYSTEM"
        );
        when(paymentService.getPaymentHistory(1L)).thenReturn(List.of(historyRecord));

        mockMvc.perform(get("/api/payments/1/audit/status-history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].newStatus").value("VALIDATED"))
            .andExpect(jsonPath("$[0].triggeredBy").value("SYSTEM"));
    }

    @Test
    @DisplayName("GET /api/payments/1/audit/validations: Success validation results")
    void getValidationResults_Success() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(samplePayment));

        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("min", 1);
        ValidationResultRecord resultRecord = ValidationResultRecord.success(
            1L, 100L, "AMOUNT_RANGE", ruleDef, 5
        );
        when(paymentService.getValidationResults(1L)).thenReturn(List.of(resultRecord));

        mockMvc.perform(get("/api/payments/1/audit/validations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ruleName").value("AMOUNT_RANGE"))
            .andExpect(jsonPath("$[0].passed").value(true));
    }

    @Test
    @DisplayName("GET /api/payments/99/audit/status-history: Returns 404 when payment not found")
    void getStatusHistory_NotFound() throws Exception {
        when(paymentService.getPaymentById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/99/audit/status-history"))
            .andExpect(status().isNotFound());
    }
}
