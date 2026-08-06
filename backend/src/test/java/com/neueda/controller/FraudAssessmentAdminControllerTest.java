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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AdminFraudDecisionRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
class FraudAssessmentAdminControllerTest {

    @Mock
    private FraudAssessmentRepository fraudAssessmentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentStatusHistoryRepository statusHistoryRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private FraudAssessmentRecord sampleAssessment;
    private PaymentRecord samplePayment;

    @BeforeEach
    void setUp() {
        FraudAssessmentAdminController controller = new FraudAssessmentAdminController(
            fraudAssessmentRepository, paymentRepository, statusHistoryRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        sampleAssessment = FraudAssessmentRecord.create(
            100L, BigDecimal.valueOf(80), BigDecimal.valueOf(80), BigDecimal.valueOf(80),
            "[\"LARGE_TX\"]", "{}", FraudDecision.SUSPICIOUS, FraudRiskLevel.HIGH, "High risk"
        );

        samplePayment = new PaymentRecord(
            100L, "KEY100", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.SUSPICIOUS, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("GET /api/admin/fraud-assessments: Success list assessments")
    void listAssessments_Success() throws Exception {
        when(fraudAssessmentRepository.findAll(any(), any(), any(), any(), eq(50), eq(0)))
            .thenReturn(List.of(sampleAssessment));

        mockMvc.perform(get("/api/admin/fraud-assessments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/fraud-assessments/pending: Success list pending reviews")
    void getPendingReviews_Success() throws Exception {
        when(fraudAssessmentRepository.countPendingReview()).thenReturn(1L);
        when(fraudAssessmentRepository.findPendingReview(50, 0)).thenReturn(List.of(sampleAssessment));

        mockMvc.perform(get("/api/admin/fraud-assessments/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/fraud-assessments/{id}: Success details")
    void getAssessmentDetails_Success() throws Exception {
        when(fraudAssessmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssessment));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        mockMvc.perform(get("/api/admin/fraud-assessments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment.id").value(100));
    }

    @Test
    @DisplayName("POST /api/admin/fraud-assessments/{id}/approve: Success approves suspicious payment")
    void approveAssessment_Success() throws Exception {
        when(fraudAssessmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssessment));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        AdminFraudDecisionRequest req = new AdminFraudDecisionRequest("ADMIN_USER", "Approved after review");

        mockMvc.perform(post("/api/admin/fraud-assessments/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("approved"));
    }

    @Test
    @DisplayName("POST /api/admin/fraud-assessments/{id}/reject: Success rejects suspicious payment")
    void rejectAssessment_Success() throws Exception {
        when(fraudAssessmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssessment));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        AdminFraudDecisionRequest req = new AdminFraudDecisionRequest("ADMIN_USER", "Fraud confirmed");

        mockMvc.perform(post("/api/admin/fraud-assessments/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("rejected"));
    }

    @Test
    @DisplayName("GET /api/admin/fraud-assessments/stats/overview: Success stats")
    void getOverview_Success() throws Exception {
        when(fraudAssessmentRepository.countByDecision(FraudDecision.APPROVED)).thenReturn(10L);
        when(fraudAssessmentRepository.countByDecision(FraudDecision.SUSPICIOUS)).thenReturn(2L);
        when(fraudAssessmentRepository.countByDecision(FraudDecision.REJECTED)).thenReturn(1L);
        when(fraudAssessmentRepository.countPendingReview()).thenReturn(2L);

        mockMvc.perform(get("/api/admin/fraud-assessments/stats/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAssessed").value(13));
    }
}
