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
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.CreatePaymentRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.AccountService;
import com.neueda.service.FraudRiskService;
import com.neueda.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private PaymentService paymentService;
    @Mock private AccountService accountService;
    @Mock private FraudRiskService fraudRiskService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PaymentRecord validatedPayment;

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService, accountService, fraudRiskService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validatedPayment = new PaymentRecord(
            1L, "IDEM123", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE, PaymentStatus.VALIDATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/payments: Creates single payment")
    void createPayment_Success() throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest(
            "111122223333", "444455556666", BigDecimal.valueOf(100), "USD", "IDEM123", "1234",
            "USD", "USD", BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE
        );

        when(accountService.verifyAccountPinByAccountNumber("111122223333", "1234")).thenReturn(true);
        when(paymentService.createPayment(any())).thenReturn(validatedPayment);
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(new FraudRiskService.PaymentRisk(0.05, false));
        when(paymentService.getValidationResults(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("GET /api/payments/1: Returns payment details")
    void getPaymentById_Success() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(validatedPayment));
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(new FraudRiskService.PaymentRisk(0.05, false));
        when(paymentService.getValidationResults(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.sourceAccount").value("111122223333"));
    }
}
