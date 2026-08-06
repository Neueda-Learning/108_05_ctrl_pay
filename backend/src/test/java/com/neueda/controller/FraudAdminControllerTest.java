package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
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
import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AdminFraudDecisionRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;
import com.neueda.service.FraudRiskService;

@ExtendWith(MockitoExtension.class)
class FraudAdminControllerTest {

    @Mock
    private FraudAssessmentRepository assessmentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentStatusHistoryRepository statusHistoryRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private FraudRiskService fraudRiskService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private FraudAssessmentRecord sampleAssessment;
    private PaymentRecord samplePayment;
    private AccountRecord sampleAccount;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        FraudAdminController controller = new FraudAdminController(
            assessmentRepository, paymentRepository, statusHistoryRepository,
            accountRepository, fraudRiskService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

        sampleAssessment = FraudAssessmentRecord.create(
            100L, BigDecimal.valueOf(80), BigDecimal.valueOf(80), BigDecimal.valueOf(80),
            "[\"LARGE_TX\"]", "{}", FraudDecision.SUSPICIOUS, FraudRiskLevel.HIGH, "High risk"
        );

        samplePayment = new PaymentRecord(
            100L, null, "111122223333", "444455556666", new BigDecimal("5000.00"), "USD",
            null, null, null, PaymentStatus.SUSPICIOUS, null, null,
            0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        sampleAccount = new AccountRecord(
            1L, 10L, "111122223333", "Checking", new BigDecimal("10000.00"), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234"
        );
    }

    @Test
    @DisplayName("GET /api/admin/fraud/stats: Returns fraud stats overview")
    void getStats_Success() throws Exception {
        when(assessmentRepository.countByDecision(FraudDecision.APPROVED)).thenReturn(10L);
        when(assessmentRepository.countByDecision(FraudDecision.SUSPICIOUS)).thenReturn(2L);
        when(assessmentRepository.countByDecision(FraudDecision.REJECTED)).thenReturn(1L);
        when(assessmentRepository.countPendingReview()).thenReturn(2L);

        mockMvc.perform(get("/api/admin/fraud/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAssessed").value(13));
    }

    @Test
    @DisplayName("GET /api/admin/fraud/transactions: Returns list of suspicious transactions")
    void getSuspiciousTransactions_Success() throws Exception {
        when(assessmentRepository.findAll(eq(FraudDecision.SUSPICIOUS), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(List.of(sampleAssessment));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(assessmentRepository.countPendingReview()).thenReturn(1L);

        mockMvc.perform(get("/api/admin/fraud/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactions[0].paymentId").value(100))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/fraud/payment/{id}: Returns full fraud investigation details")
    void getFraudInvestigationDetails_Success() throws Exception {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(assessmentRepository.findByPaymentId(100L)).thenReturn(Optional.of(sampleAssessment));
        when(accountRepository.findByAccountNumber("111122223333")).thenReturn(Optional.of(sampleAccount));

        mockMvc.perform(get("/api/admin/fraud/payment/100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment.id").value(100))
            .andExpect(jsonPath("$.fraudAssessment.paymentId").value(100));
    }

    @Test
    @DisplayName("POST /api/admin/fraud/payment/{id}/approve: Approves suspicious payment")
    void approveSuspiciousPayment_Success() throws Exception {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(assessmentRepository.findByPaymentId(100L)).thenReturn(Optional.of(sampleAssessment));

        AdminFraudDecisionRequest request = new AdminFraudDecisionRequest("Admin approves", "ADMIN1");

        mockMvc.perform(post("/api/admin/fraud/payment/100/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("approved"))
            .andExpect(jsonPath("$.newStatus").value("VALIDATED"));

        verify(paymentRepository).update(any());
    }

    @Test
    @DisplayName("POST /api/admin/fraud/payment/{id}/reject: Rejects suspicious payment")
    void rejectSuspiciousPayment_Success() throws Exception {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        when(assessmentRepository.findByPaymentId(100L)).thenReturn(Optional.of(sampleAssessment));

        AdminFraudDecisionRequest request = new AdminFraudDecisionRequest("High risk confirmed", "ADMIN1");

        mockMvc.perform(post("/api/admin/fraud/payment/100/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("rejected"));

        verify(paymentRepository).update(any());
    }
}
