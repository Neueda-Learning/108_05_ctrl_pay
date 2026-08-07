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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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

    @Mock
    private PaymentService paymentService;
    @Mock
    private AccountService accountService;
    @Mock
    private FraudRiskService fraudRiskService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PaymentRecord samplePayment;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PaymentController controller = new PaymentController(
            paymentService, accountService, fraudRiskService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

        samplePayment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/payments: Success create payment")
    void createPayment_Success() throws Exception {
        when(accountService.verifyAccountPinByAccountNumber("111122223333", "1234")).thenReturn(true);
        when(paymentService.createPayment(any())).thenReturn(samplePayment);
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(new FraudRiskService.PaymentRisk(0.0, false));
        when(paymentService.getValidationResults(1L)).thenReturn(List.of());

        CreatePaymentRequest req = new CreatePaymentRequest(
            "111122223333", "444455556666", BigDecimal.valueOf(500), "USD", "KEY1", "1234",
            "USD", "USD", BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE
        );

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/payments/{id}: Success get payment")
    void getPayment_Success() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(samplePayment));
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(new FraudRiskService.PaymentRisk(0.0, false));
        when(paymentService.getValidationResults(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/payments/{id}: Returns 404 when payment not found")
    void getPayment_NotFound() throws Exception {
        when(paymentService.getPaymentById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/payments: Success list payments")
    void listPayments_Success() throws Exception {
        when(paymentService.listPaymentsFiltered(any(), any(), any(), any(), any(), any(), eq(10), eq(0)))
            .thenReturn(List.of(samplePayment));
        when(fraudRiskService.assessPaymentRisk(any())).thenReturn(new FraudRiskService.PaymentRisk(0.0, false));
        when(paymentService.getValidationResults(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }
}
