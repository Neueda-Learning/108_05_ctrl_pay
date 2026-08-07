package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.StatusTransitionRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.FraudRiskService;
import com.neueda.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleControllerTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private FraudRiskService fraudRiskService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private PaymentRecord createdPayment;
    private PaymentRecord validatedPayment;
    private PaymentRecord sentPayment;

    @BeforeEach
    void setUp() {
        PaymentLifecycleController controller = new PaymentLifecycleController(paymentService, fraudRiskService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        createdPayment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.CREATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        validatedPayment = createdPayment.withStatus(PaymentStatus.VALIDATED);
        sentPayment = createdPayment.withStatus(PaymentStatus.SENT);
    }

    @Test
    @DisplayName("POST /api/payments/1/validate: Success validates payment")
    void validatePayment_Success() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(createdPayment));
        when(paymentService.transitionPayment(1L, PaymentStatus.VALIDATED)).thenReturn(validatedPayment);
        
        FraudRiskService.PaymentRisk risk = new FraudRiskService.PaymentRisk(5.0, false);
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(risk);
        when(paymentService.getValidationResults(1L)).thenReturn(List.of());

        mockMvc.perform(post("/api/payments/1/validate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("POST /api/payments/1/validate: Invalid status returns 400")
    void validatePayment_InvalidState() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(validatedPayment));

        mockMvc.perform(post("/api/payments/1/validate"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payments/1/fail: Success manually fails payment")
    void failPayment_Success() throws Exception {
        PaymentRecord failedPayment = createdPayment.withFailure("MANUAL_FAILURE", "Failed manually");
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(createdPayment));
        when(paymentService.failPayment(eq(1L), any(), any())).thenReturn(failedPayment);
        
        FraudRiskService.PaymentRisk risk = new FraudRiskService.PaymentRisk(5.0, false);
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(risk);
        when(paymentService.getValidationResults(1L)).thenReturn(List.of());

        StatusTransitionRequest req = new StatusTransitionRequest("MANUAL_FAILURE", "Failed manually");

        mockMvc.perform(post("/api/payments/1/fail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
