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

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

class BehavioralBaselineRuleTest {

    private BehavioralBaselineRule rule;
    private PaymentRecord payment;

    @BeforeEach
    void setUp() {
        rule = new BehavioralBaselineRule();
        payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD",
            null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now());
    }

    private AccountRecord createAccount(String accNo, AccountStatus status) {
        return new AccountRecord(1L, 10L, accNo, "Test Acc", new BigDecimal("1000"), status, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Source SUSPICIOUS triggers high score")
    void evaluate_SourceSuspicious() {
        AccountRecord src = createAccount("ACC-01", AccountStatus.SUSPICIOUS);
        AccountRecord dst = createAccount("ACC-02", AccountStatus.ACTIVE);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(60, result.score());
    }

    @Test
    @DisplayName("evaluate: Destination SUSPICIOUS triggers score")
    void evaluate_DestinationSuspicious() {
        AccountRecord src = createAccount("ACC-01", AccountStatus.ACTIVE);
        AccountRecord dst = createAccount("ACC-02", AccountStatus.SUSPICIOUS);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(50, result.score());
    }

    @Test
    @DisplayName("evaluate: Source DORMANT triggers score")
    void evaluate_SourceDormant() {
        AccountRecord src = createAccount("ACC-01", AccountStatus.DORMANT);
        AccountRecord dst = createAccount("ACC-02", AccountStatus.ACTIVE);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(30, result.score());
    }

    @Test
    @DisplayName("evaluate: Destination DORMANT triggers score")
    void evaluate_DestinationDormant() {
        AccountRecord src = createAccount("ACC-01", AccountStatus.ACTIVE);
        AccountRecord dst = createAccount("ACC-02", AccountStatus.DORMANT);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(20, result.score());
    }

    @Test
    @DisplayName("evaluate: Both ACTIVE does not trigger")
    void evaluate_Normal() {
        AccountRecord src = createAccount("ACC-01", AccountStatus.ACTIVE);
        AccountRecord dst = createAccount("ACC-02", AccountStatus.ACTIVE);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("BEHAVIORAL_BASELINE", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.0, rule.getWeight());
    }
}
