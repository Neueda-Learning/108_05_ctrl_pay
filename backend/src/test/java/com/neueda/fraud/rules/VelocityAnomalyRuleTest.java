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
import org.springframework.test.util.ReflectionTestUtils;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

class VelocityAnomalyRuleTest {

    private VelocityAnomalyRule rule;
    private AccountRecord dst;

    @BeforeEach
    void setUp() {
        rule = new VelocityAnomalyRule();
        ReflectionTestUtils.setField(rule, "highRatioThreshold", 0.5);
        ReflectionTestUtils.setField(rule, "extremeRatioThreshold", 0.9);
        ReflectionTestUtils.setField(rule, "weight", 0.06);

        dst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    private AccountRecord srcWithBalance(BigDecimal balance) {
        return new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", balance, AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Ratio >= 90% triggers score 40")
    void evaluate_ExtremeRatio() {
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("950"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());

        FraudRuleResult result = rule.evaluate(payment, srcWithBalance(new BigDecimal("1000")), dst);

        assertTrue(result.triggered());
        assertEquals(40, result.score());
    }

    @Test
    @DisplayName("evaluate: Ratio >= 50% triggers score 20")
    void evaluate_HighRatio() {
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("600"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());

        FraudRuleResult result = rule.evaluate(payment, srcWithBalance(new BigDecimal("1000")), dst);

        assertTrue(result.triggered());
        assertEquals(20, result.score());
    }

    @Test
    @DisplayName("evaluate: Ratio < 50% does not trigger")
    void evaluate_NormalRatio() {
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("200"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());

        FraudRuleResult result = rule.evaluate(payment, srcWithBalance(new BigDecimal("1000")), dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("evaluate: Zero or null balance does not trigger")
    void evaluate_ZeroBalance() {
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("200"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());

        FraudRuleResult result = rule.evaluate(payment, srcWithBalance(BigDecimal.ZERO), dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("VELOCITY_ANOMALY", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.06, rule.getWeight());
    }
}
