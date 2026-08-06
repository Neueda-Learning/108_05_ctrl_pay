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

class AccountDifferenceRuleTest {

    private AccountDifferenceRule rule;
    private ObjectMapper objectMapper;
    private ObjectNode ruleDefinition;

    @BeforeEach
    void setUp() {
        rule = new AccountDifferenceRule();
        objectMapper = new ObjectMapper();
        ruleDefinition = objectMapper.createObjectNode();
        ruleDefinition.put("message", "Source and destination accounts must be different");
    }

    @Test
    @DisplayName("execute: Passes when source and destination accounts differ")
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
    @DisplayName("execute: Fails when source and destination accounts are identical")
    void execute_SameAccounts() {
        PaymentRecord payment = new PaymentRecord(
            1L, "K", "111122223333", "111122223333", BigDecimal.TEN, "USD",
            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_ACCOUNT");
        assertThat(result.errorMessage()).isEqualTo("Source and destination accounts must be different");
    }

    @Test
    @DisplayName("execute: Fails when source or destination account is null")
    void execute_NullAccount() {
        PaymentRecord payment = new PaymentRecord(
            1L, "K", null, "444455556666", BigDecimal.TEN, "USD",
            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isFalse();
        assertThat(result.errorMessage()).contains("Source or destination account is null");
    }
}
