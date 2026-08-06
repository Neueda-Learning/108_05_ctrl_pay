package com.neueda.validation.rules.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.validation.rules.ValidationRule.ValidationRuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class AmountRangeRuleTest {

    private AmountRangeRule rule;
    private ObjectMapper objectMapper;
    private ObjectNode ruleDefinition;

    @BeforeEach
    void setUp() {
        rule = new AmountRangeRule();
        objectMapper = new ObjectMapper();
        ruleDefinition = objectMapper.createObjectNode();
        ruleDefinition.put("min", 1.00);
        ruleDefinition.put("max", 1000.00);
        ruleDefinition.put("message", "Amount out of range");
    }

    @Test
    @DisplayName("execute: Passes when amount is within min and max")
    void execute_Pass() {
        PaymentRecord payment = PaymentRecord.create(null, "111122223333", "444455556666", BigDecimal.valueOf(500), "USD");

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("execute: Fails when amount is above max limit")
    void execute_ExceedsMax() {
        PaymentRecord payment = PaymentRecord.create(null, "111122223333", "444455556666", BigDecimal.valueOf(2000), "USD");

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_AMOUNT");
    }
}
