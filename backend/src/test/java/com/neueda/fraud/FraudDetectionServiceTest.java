package com.neueda.fraud;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.fraud.rules.FraudRuleEngine;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.service.FraudRiskService;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudRuleEngine ruleEngine;
    @Mock
    private FraudDecisionEngine decisionEngine;
    @Mock
    private FraudAssessmentRepository assessmentRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private FraudRiskService fraudRiskService;

    private ObjectMapper objectMapper;
    private FraudDetectionService service;

    private PaymentRecord payment;
    private AccountRecord sourceAcc;
    private AccountRecord destAcc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new FraudDetectionService(
            ruleEngine, decisionEngine, assessmentRepository,
            accountRepository, objectMapper, fraudRiskService
        );
        ReflectionTestUtils.setField(service, "fraudDetectionEnabled", true);

        payment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        sourceAcc = new AccountRecord(
            10L, 1L, "ACC1", "Source", BigDecimal.valueOf(5000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );
        destAcc = new AccountRecord(
            20L, 2L, "ACC2", "Dest", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );
    }

    @Test
    @DisplayName("assessPayment: Returns existing assessment if already present")
    void assessPayment_IdempotentExisting() {
        // Arrange
        FraudAssessmentRecord existing = FraudAssessmentRecord.create(
            1L, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, "[]", "{}",
            FraudDecision.APPROVED, FraudRiskLevel.LOW, "Existing"
        );
        when(assessmentRepository.findByPaymentId(1L)).thenReturn(Optional.of(existing));

        // Act
        FraudAssessmentRecord result = service.assessPayment(payment);

        // Assert
        assertEquals(existing, result);
    }

    @Test
    @DisplayName("assessPayment: Performs complete assessment when not existing")
    void assessPayment_NewAssessment_Success() {
        // Arrange
        when(assessmentRepository.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1")).thenReturn(Optional.of(sourceAcc));
        when(accountRepository.findByAccountNumber("ACC2")).thenReturn(Optional.of(destAcc));

        FraudRuleEngine.RuleScoreDetail detail = new FraudRuleEngine.RuleScoreDetail("LARGE_TX", 20, true, "Large", 1.0);
        FraudRuleEngine.FraudDetectionResult ruleResult = new FraudRuleEngine.FraudDetectionResult(
            BigDecimal.valueOf(20), java.util.List.of(), java.util.List.of("LARGE_TX"),
            Map.of("LARGE_TX", detail), "Explanation"
        );
        when(ruleEngine.evaluatePayment(payment, sourceAcc, destAcc)).thenReturn(ruleResult);

        FraudRiskService.PaymentRisk risk = new FraudRiskService.PaymentRisk(15.0, false);
        when(fraudRiskService.assessPaymentRiskForCreation(payment)).thenReturn(risk);

        FraudDecisionEngine.FraudDecisionResult decisionResult = new FraudDecisionEngine.FraudDecisionResult(
            FraudDecision.APPROVED, FraudRiskLevel.LOW, BigDecimal.valueOf(17), BigDecimal.valueOf(20), BigDecimal.valueOf(15), "OK"
        );
        when(decisionEngine.makeDecision(BigDecimal.valueOf(20), BigDecimal.valueOf(15.0))).thenReturn(decisionResult);

        FraudAssessmentRecord savedRecord = FraudAssessmentRecord.create(
            1L, BigDecimal.valueOf(17), BigDecimal.valueOf(20), BigDecimal.valueOf(15),
            "[\"LARGE_TX\"]", "{}", FraudDecision.APPROVED, FraudRiskLevel.LOW, "OK"
        );
        when(assessmentRepository.save(any())).thenReturn(savedRecord);

        // Act
        FraudAssessmentRecord result = service.assessPayment(payment);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.paymentId());
        verify(assessmentRepository).save(any());
    }

    @Test
    @DisplayName("assessPayment: When fraud detection disabled, returns default APPROVED")
    void assessPayment_Disabled() {
        // Arrange
        ReflectionTestUtils.setField(service, "fraudDetectionEnabled", false);

        // Act
        FraudAssessmentRecord result = service.assessPayment(payment);

        // Assert
        assertEquals(FraudDecision.APPROVED, result.decision());
        assertEquals("Fraud detection disabled", result.explanation());
    }

    @Test
    @DisplayName("assessPayment: Throws IllegalArgumentException when account not found")
    void assessPayment_AccountNotFound() {
        // Arrange
        when(assessmentRepository.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.assessPayment(payment));
    }

    @Test
    @DisplayName("shouldAssess: Returns true for valid payment with id")
    void shouldAssess_Valid() {
        assertTrue(service.shouldAssess(payment));
        assertFalse(service.shouldAssess(null));
    }
}
