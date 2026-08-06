package com.neueda.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;
import com.neueda.service.FraudRiskService;

@ExtendWith(MockitoExtension.class)
class FraudAdminControllerTest {

    @Mock private FraudAssessmentRepository assessmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentStatusHistoryRepository statusHistoryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private FraudRiskService fraudRiskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FraudAdminController controller = new FraudAdminController(
            assessmentRepository, paymentRepository, statusHistoryRepository, accountRepository, fraudRiskService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/admin/fraud/transactions: Returns status 200 OK")
    void getTransactions_Success() throws Exception {
        mockMvc.perform(get("/api/admin/fraud/transactions"))
            .andExpect(status().isOk());
    }
}
