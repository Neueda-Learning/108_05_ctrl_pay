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

class ContextualRiskAggregationRuleTest {

    private ContextualRiskAggregationRule rule;

    @BeforeEach
    void setUp() {
        rule = new ContextualRiskAggregationRule();
        ReflectionTestUtils.setField(rule, "largeAmountThreshold", 50000.0);
        ReflectionTestUtils.setField(rule, "lowDestBalanceThreshold", 500.0);
        ReflectionTestUtils.setField(rule, "weight", 0.06);
    }

    private AccountRecord createAccount(String accNo, String currency, BigDecimal balance) {
        return new AccountRecord(1L, 10L, accNo, "Test Acc", balance, AccountStatus.ACTIVE, currency, LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    private PaymentRecord createPayment(BigDecimal amount) {
        return new PaymentRecord(1L, null, "ACC-01", "ACC-02", amount, "USD",
            null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("evaluate: 3 or more risk factors triggers high score")
    void evaluate_ThreeOrMoreRiskFactors() {
        PaymentRecord payment = createPayment(new BigDecimal("60000"));
        AccountRecord src = createAccount("ACC-01", "USD", new BigDecimal("100000"));
        AccountRecord dst = createAccount("ACC-02", "EUR", new BigDecimal("100"));

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(100, result.score());
    }

    @Test
    @DisplayName("evaluate: Exactly 2 risk factors triggers score 20")
    void evaluate_TwoRiskFactors() {
        PaymentRecord payment = createPayment(new BigDecimal("3000"));
        AccountRecord src = createAccount("ACC-01", "USD", new BigDecimal("5000"));
        AccountRecord dst = createAccount("ACC-02", "EUR", new BigDecimal("2000"));

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(20, result.score());
    }

    @Test
    @DisplayName("evaluate: 1 or 0 risk factors does not trigger")
    void evaluate_LowRisk() {
        PaymentRecord payment = createPayment(new BigDecimal("100"));
        AccountRecord src = createAccount("ACC-01", "USD", new BigDecimal("5000"));
        AccountRecord dst = createAccount("ACC-02", "USD", new BigDecimal("2000"));

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("CONTEXTUAL_RISK_AGGREGATION", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.06, rule.getWeight());
    }
}
