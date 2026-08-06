package com.neueda.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.FraudDecision;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.DashboardOverviewDTO;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("getDashboardOverview: Calculates metrics and returns system health OK when success rate > 90%")
    void getDashboardOverview_Success() {
        when(paymentRepository.count()).thenReturn(100L);
        when(paymentRepository.countByStatus(PaymentStatus.COMPLETED)).thenReturn(95L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(3L);
        when(paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS)).thenReturn(2L);
        when(fraudAssessmentRepository.countByDecision(FraudDecision.REJECTED)).thenReturn(1L);
        when(fraudAssessmentRepository.countByDecision(FraudDecision.SUSPICIOUS)).thenReturn(2L);

        DashboardOverviewDTO overview = analyticsService.getDashboardOverview();

        assertThat(overview.totalTransactions()).isEqualTo(100L);
        assertThat(overview.successfulTransactions()).isEqualTo(95L);
        assertThat(overview.transactionSuccessRate()).isEqualTo(95.0);
        assertThat(overview.systemHealth()).isEqualTo("OK");
    }

    @Test
    @DisplayName("getDashboardOverview: Returns WARNING when success rate is between 70% and 90%")
    void getDashboardOverview_Warning() {
        when(paymentRepository.count()).thenReturn(100L);
        when(paymentRepository.countByStatus(PaymentStatus.COMPLETED)).thenReturn(80L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(20L);

        DashboardOverviewDTO overview = analyticsService.getDashboardOverview();

        assertThat(overview.systemHealth()).isEqualTo("WARNING");
    }
}
