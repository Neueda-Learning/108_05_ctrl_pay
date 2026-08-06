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

class AccountFormatRuleTest {

    private AccountFormatRule rule;
    private ObjectMapper objectMapper;
    private ObjectNode ruleDefinition;

    @BeforeEach
    void setUp() {
        rule = new AccountFormatRule();
        objectMapper = new ObjectMapper();
        ruleDefinition = objectMapper.createObjectNode();
        ruleDefinition.put("pattern", "^[0-9]{12}$");
        ruleDefinition.put("message", "Invalid account format");
    }

    @Test
    @DisplayName("execute: Passes when both accounts are 12 digits")
    void execute_Pass() {
        PaymentRecord payment = new PaymentRecord(
            1L, "K", "111122223333", "444455556666", BigDecimal.TEN, "USD",
            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("execute: Fails when source account is not 12 digits")
    void execute_InvalidFormat() {
        PaymentRecord payment = new PaymentRecord(
            1L, "K", "123", "444455556666", BigDecimal.TEN, "USD",
            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_ACCOUNT");
    }
}
