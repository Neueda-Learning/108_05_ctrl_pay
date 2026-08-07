package com.neueda.validation.rules.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.validation.rules.ValidationRule;

class MockSufficientFundsRuleTest {

    private MockSufficientFundsRule rule;
    private ObjectMapper objectMapper;
    private PaymentRecord payment;

    @BeforeEach
    void setUp() {
        rule = new MockSufficientFundsRule();
        objectMapper = new ObjectMapper();
        payment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.CREATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("MockSufficientFundsRule: Executes rule definition with zero failure rate")
    void execute_ZeroFailureRate() {
        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("failure_rate", 0.0);
        ruleDef.put("message", "Insufficient balance");

        ValidationRule.ValidationRuleResult result = rule.execute(payment, ruleDef);
        assertNotNull(result);
    }

    @Test
    @DisplayName("MockSufficientFundsRule: Executes rule definition with 100% failure rate")
    void execute_AlwaysFailRate() {
        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("failure_rate", 1.0);
        ruleDef.put("message", "Insufficient balance");

        ValidationRule.ValidationRuleResult result = rule.execute(payment, ruleDef);
        assertNotNull(result);
    }
}
