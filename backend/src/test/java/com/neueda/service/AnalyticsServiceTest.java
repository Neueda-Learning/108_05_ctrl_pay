package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.FraudDecision;
import com.neueda.domain.MLModelRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.BulkPaymentAnalyticsDashboardDTO;
import com.neueda.dto.ComplianceDashboardDTO;
import com.neueda.dto.CustomerAnalyticsDashboardDTO;
import com.neueda.dto.DashboardOverviewDTO;
import com.neueda.dto.FraudDashboardDTO;
import com.neueda.dto.MLModelDashboardDTO;
import com.neueda.dto.TransactionDashboardDTO;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private com.neueda.repository.PaymentRepository paymentRepository;
    @Mock private com.neueda.repository.CustomerRepository customerRepository;
    @Mock private com.neueda.repository.AccountRepository accountRepository;
    @Mock private com.neueda.repository.FraudAssessmentRepository fraudAssessmentRepository;
    @Mock private com.neueda.repository.FraudAuditEventRepository fraudAuditEventRepository;
    @Mock private com.neueda.repository.FraudRuleRepository fraudRuleRepository;
    @Mock private com.neueda.repository.BulkPaymentBatchRepository bulkPaymentBatchRepository;
    @Mock private com.neueda.repository.BulkPaymentItemRepository bulkPaymentItemRepository;
    @Mock private com.neueda.repository.MLModelRepository mlModelRepository;
    @Mock private com.neueda.repository.MLModelPredictionRepository mlModelPredictionRepository;
    @Mock private com.neueda.repository.ValidationResultRepository validationResultRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
            paymentRepository, customerRepository, accountRepository, fraudAssessmentRepository,
            fraudAuditEventRepository, fraudRuleRepository, bulkPaymentBatchRepository,
            bulkPaymentItemRepository, mlModelRepository, mlModelPredictionRepository, validationResultRepository
        );
    }

    @Test
    @DisplayName("getDashboardOverview: Success rate > 90% -> OK")
    void getDashboardOverview_Success() {
        when(paymentRepository.count()).thenReturn(100L);
        when(paymentRepository.countByStatus(PaymentStatus.COMPLETED)).thenReturn(95L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(3L);
        when(paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS)).thenReturn(2L);

        DashboardOverviewDTO overview = analyticsService.getDashboardOverview();

        assertThat(overview.totalTransactions()).isEqualTo(100L);
        assertThat(overview.systemHealth()).isEqualTo("OK");
    }

    @Test
    @DisplayName("getDashboardOverview: Success rate 70-90% -> WARNING")
    void getDashboardOverview_Warning() {
        when(paymentRepository.count()).thenReturn(100L);
        when(paymentRepository.countByStatus(PaymentStatus.COMPLETED)).thenReturn(80L);

        DashboardOverviewDTO overview = analyticsService.getDashboardOverview();

        assertThat(overview.systemHealth()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("getDashboardOverview: Success rate < 70% -> CRITICAL")
    void getDashboardOverview_Critical() {
        when(paymentRepository.count()).thenReturn(100L);
        when(paymentRepository.countByStatus(PaymentStatus.COMPLETED)).thenReturn(50L);

        DashboardOverviewDTO overview = analyticsService.getDashboardOverview();

        assertThat(overview.systemHealth()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("getTransactionDashboard: Returns counts by status")
    void getTransactionDashboard_Success() {
        when(paymentRepository.count()).thenReturn(50L);
        when(paymentRepository.countByStatus(PaymentStatus.CREATED)).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.VALIDATED)).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.SENT)).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.COMPLETED)).thenReturn(30L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS)).thenReturn(5L);

        TransactionDashboardDTO dto = analyticsService.getTransactionDashboard();

        assertThat(dto.totalTransactionCount()).isEqualTo(50L);
        assertThat(dto.transactionsByStatus().get("COMPLETED")).isEqualTo(30L);
    }

    @Test
    @DisplayName("getFraudDashboard: Calculates fraud stats")
    void getFraudDashboard_Success() {
        when(fraudAssessmentRepository.countByDecision(FraudDecision.APPROVED)).thenReturn(80L);
        when(fraudAssessmentRepository.countByDecision(FraudDecision.REJECTED)).thenReturn(10L);
        when(fraudAssessmentRepository.countByDecision(FraudDecision.SUSPICIOUS)).thenReturn(10L);

        FraudDashboardDTO dto = analyticsService.getFraudDashboard();

        assertThat(dto.totalFraudChecks()).isEqualTo(100L);
        assertThat(dto.fraudPreventionPercentage()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("getCustomerAnalyticsDashboard: Returns default customer dashboard")
    void getCustomerAnalyticsDashboard_Success() {
        CustomerAnalyticsDashboardDTO dto = analyticsService.getCustomerAnalyticsDashboard();

        assertThat(dto).isNotNull();
        assertThat(dto.totalCustomers()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getBulkPaymentAnalyticsDashboard: Returns bulk payment metrics")
    void getBulkPaymentAnalyticsDashboard_Success() {
        BulkPaymentAnalyticsDashboardDTO dto = analyticsService.getBulkPaymentAnalyticsDashboard();

        assertThat(dto).isNotNull();
        assertThat(dto.totalBatchesCreated()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getMLModelDashboard: With active model")
    void getMLModelDashboard_ActiveModel() {
        MLModelRecord activeModel = MLModelRecord.create(
            "FraudPredictor", "v1.0", "Desc", "XGBOOST", "path/to/model",
            LocalDateTime.now(), "dataset1", 1000,
            BigDecimal.valueOf(0.95), BigDecimal.valueOf(0.94), BigDecimal.valueOf(0.93),
            BigDecimal.valueOf(0.935), BigDecimal.valueOf(0.98), "PROD", "ADMIN"
        );
        when(mlModelRepository.findActiveModel()).thenReturn(Optional.of(activeModel));

        MLModelDashboardDTO dto = analyticsService.getMLModelDashboard();

        assertThat(dto.activeModelName()).isEqualTo("FraudPredictor");
        assertThat(dto.accuracy()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("getMLModelDashboard: No active model returns fallback")
    void getMLModelDashboard_NoActiveModel() {
        when(mlModelRepository.findActiveModel()).thenReturn(Optional.empty());

        MLModelDashboardDTO dto = analyticsService.getMLModelDashboard();

        assertThat(dto.activeModelName()).isEqualTo("No Active Model");
    }

    @Test
    @DisplayName("getComplianceDashboard: Returns compliance statistics")
    void getComplianceDashboard_Success() {
        when(fraudAssessmentRepository.countPendingReview()).thenReturn(5L);

        ComplianceDashboardDTO dto = analyticsService.getComplianceDashboard();

        assertThat(dto.pendingReviews()).isEqualTo(5L);
    }
}
