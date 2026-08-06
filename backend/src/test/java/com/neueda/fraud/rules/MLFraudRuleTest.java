package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.service.FraudRiskService;

@ExtendWith(MockitoExtension.class)
class MLFraudRuleTest {

    @Mock
    private FraudRiskService fraudRiskService;

    private MLFraudRule rule;
    private PaymentRecord payment;
    private AccountRecord src;
    private AccountRecord dst;

    @BeforeEach
    void setUp() {
        rule = new MLFraudRule(fraudRiskService);
        ReflectionTestUtils.setField(rule, "weight", 0.25);

        payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD",
            null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now());
        src = new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
        dst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: High ML probability triggers rule")
    void evaluate_HighProbability() {
        FraudRiskService.PaymentRisk risk = new FraudRiskService.PaymentRisk(75.0, true);
        when(fraudRiskService.assessPaymentRiskForCreation(payment)).thenReturn(risk);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(75, result.score());
    }

    @Test
    @DisplayName("evaluate: Low ML probability does not trigger rule")
    void evaluate_LowProbability() {
        FraudRiskService.PaymentRisk risk = new FraudRiskService.PaymentRisk(20.0, false);
        when(fraudRiskService.assessPaymentRiskForCreation(payment)).thenReturn(risk);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("evaluate: Null ML probability does not trigger rule")
    void evaluate_NullProbability() {
        FraudRiskService.PaymentRisk risk = new FraudRiskService.PaymentRisk(null, false);
        when(fraudRiskService.assessPaymentRiskForCreation(payment)).thenReturn(risk);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("evaluate: Handles Exception from FraudRiskService gracefully")
    void evaluate_ServiceException() {
        when(fraudRiskService.assessPaymentRiskForCreation(payment)).thenThrow(new RuntimeException("ML service offline"));

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("ML_FRAUD_PREDICTION", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.25, rule.getWeight());
    }
}
