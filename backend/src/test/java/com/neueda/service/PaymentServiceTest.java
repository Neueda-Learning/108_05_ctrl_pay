package com.neueda.service;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.ValidationResultRecord;
import com.neueda.domain.ValidationRuleRecord;
import com.neueda.fraud.FraudDetectionService;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;
import com.neueda.repository.ValidationResultRepository;
import com.neueda.repository.ValidationRuleRepository;
import com.neueda.validation.RuleEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ValidationRuleRepository validationRuleRepository;
    @Mock private ValidationResultRepository validationResultRepository;
    @Mock private PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    @Mock private RuleEngine ruleEngine;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private FraudAssessmentRepository fraudAssessmentRepository;

    private PaymentService paymentService;

    private PaymentRecord newPayment;
    private PaymentRecord savedPayment;
    private FraudAssessmentRecord approvedAssessment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            paymentRepository, validationRuleRepository, validationResultRepository,
            paymentStatusHistoryRepository, ruleEngine, fraudDetectionService, fraudAssessmentRepository
        );

        newPayment = new PaymentRecord(
            null, "IDEM123", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        savedPayment = new PaymentRecord(
            1L, "IDEM123", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        approvedAssessment = FraudAssessmentRecord.create(
            1L, BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(0.1),
            "[]", "{}", FraudDecision.APPROVED, FraudRiskLevel.LOW, "Low risk"
        );
    }

    @Test
    @DisplayName("createPayment: Idempotent - returns existing payment if idempotency key exists")
    void createPayment_Idempotency() {
        when(paymentRepository.findByIdempotencyKey("IDEM123")).thenReturn(Optional.of(savedPayment));

        PaymentRecord result = paymentService.createPayment(newPayment);

        assertThat(result.id()).isEqualTo(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPayment: Happy path - validates rules, passes fraud check, transitions to VALIDATED")
    void createPayment_Success() {
        when(paymentRepository.findByIdempotencyKey("IDEM123")).thenReturn(Optional.empty());
        List<ValidationRuleRecord> rules = List.of();
        when(validationRuleRepository.findActiveRules()).thenReturn(rules);
        List<ValidationResultRecord> results = List.of();
        when(ruleEngine.validatePayment(any(), any())).thenReturn(results);
        when(ruleEngine.hasPassedValidation(results, rules)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(fraudDetectionService.assessPayment(any())).thenReturn(approvedAssessment);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(savedPayment));
        when(paymentRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRecord result = paymentService.createPayment(newPayment);

        assertThat(result.status()).isEqualTo(PaymentStatus.VALIDATED);
        verify(paymentRepository).save(any());
    }

    @Test
    @DisplayName("createPayment: Fraud SUSPICIOUS decision moves payment to SUSPICIOUS status")
    void createPayment_FraudSuspicious() {
        FraudAssessmentRecord suspiciousAssessment = FraudAssessmentRecord.create(
            1L, BigDecimal.valueOf(75), BigDecimal.valueOf(75), BigDecimal.valueOf(0.75),
            "[]", "{}", FraudDecision.SUSPICIOUS, FraudRiskLevel.HIGH, "High velocity"
        );

        when(paymentRepository.findByIdempotencyKey("IDEM123")).thenReturn(Optional.empty());
        when(validationRuleRepository.findActiveRules()).thenReturn(List.of());
        when(ruleEngine.validatePayment(any(), any())).thenReturn(List.of());
        when(ruleEngine.hasPassedValidation(any(), any())).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(savedPayment);
        when(fraudDetectionService.assessPayment(any())).thenReturn(suspiciousAssessment);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(savedPayment));
        when(paymentRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        PaymentRecord result = paymentService.createPayment(newPayment);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("createPayment: Hard rule validation failure sets status to FAILED")
    void createPayment_ValidationFailure() {
        when(paymentRepository.findByIdempotencyKey("IDEM123")).thenReturn(Optional.empty());
        when(validationRuleRepository.findActiveRules()).thenReturn(List.of());
        when(ruleEngine.validatePayment(any(), any())).thenReturn(List.of());
        when(ruleEngine.hasPassedValidation(any(), any())).thenReturn(false);
        when(ruleEngine.aggregateErrorMessage(any())).thenReturn("Account balance insufficient");
        PaymentRecord failedSaved = savedPayment.withFailure("VALIDATION_FAILED", "Account balance insufficient");
        when(paymentRepository.save(any())).thenReturn(failedSaved);

        PaymentRecord result = paymentService.createPayment(newPayment);

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("transitionPayment: Throws IllegalStateException on invalid transition")
    void transitionPayment_Invalid() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(savedPayment));

        assertThatThrownBy(() -> paymentService.transitionPayment(1L, PaymentStatus.COMPLETED))
            .isInstanceOf(IllegalStateException.class);
    }
}
