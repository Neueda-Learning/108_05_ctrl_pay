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

class SuspiciousAccountRuleTest {

    private SuspiciousAccountRule rule;
    private PaymentRecord payment;

    @BeforeEach
    void setUp() {
        rule = new SuspiciousAccountRule();
        ReflectionTestUtils.setField(rule, "weight", 0.20);
        payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD",
            null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now());
    }

    private AccountRecord acc(String no, AccountStatus status) {
        return new AccountRecord(1L, 10L, no, "Test Acc", new BigDecimal("1000"), status, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Source SUSPICIOUS gives score 100")
    void evaluate_SourceSuspicious() {
        FraudRuleResult result = rule.evaluate(payment, acc("ACC-01", AccountStatus.SUSPICIOUS), acc("ACC-02", AccountStatus.ACTIVE));
        assertTrue(result.triggered());
        assertEquals(100, result.score());
    }

    @Test
    @DisplayName("evaluate: Destination SUSPICIOUS gives score 50")
    void evaluate_DestinationSuspicious() {
        FraudRuleResult result = rule.evaluate(payment, acc("ACC-01", AccountStatus.ACTIVE), acc("ACC-02", AccountStatus.SUSPICIOUS));
        assertTrue(result.triggered());
        assertEquals(50, result.score());
    }

    @Test
    @DisplayName("evaluate: Source DORMANT gives score 30")
    void evaluate_SourceDormant() {
        FraudRuleResult result = rule.evaluate(payment, acc("ACC-01", AccountStatus.DORMANT), acc("ACC-02", AccountStatus.ACTIVE));
        assertTrue(result.triggered());
        assertEquals(30, result.score());
    }

    @Test
    @DisplayName("evaluate: Source PASSIVE gives score 15")
    void evaluate_SourcePassive() {
        FraudRuleResult result = rule.evaluate(payment, acc("ACC-01", AccountStatus.PASSIVE), acc("ACC-02", AccountStatus.ACTIVE));
        assertTrue(result.triggered());
        assertEquals(15, result.score());
    }

    @Test
    @DisplayName("evaluate: Normal status does not trigger")
    void evaluate_Normal() {
        FraudRuleResult result = rule.evaluate(payment, acc("ACC-01", AccountStatus.ACTIVE), acc("ACC-02", AccountStatus.ACTIVE));
        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("SUSPICIOUS_ACCOUNT", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.20, rule.getWeight());
    }
}
