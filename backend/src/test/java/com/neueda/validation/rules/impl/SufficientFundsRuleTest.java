package com.neueda.validation.rules.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.service.AccountService;
import com.neueda.validation.rules.ValidationRule.ValidationRuleResult;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SufficientFundsRuleTest {

    @Mock
    private AccountService accountService;

    private SufficientFundsRule rule;
    private ObjectMapper objectMapper;
    private ObjectNode ruleDefinition;
    private AccountRecord sampleAccount;

    @BeforeEach
    void setUp() {
        rule = new SufficientFundsRule(accountService);
        objectMapper = new ObjectMapper();
        ruleDefinition = objectMapper.createObjectNode();
        ruleDefinition.put("message", "Source account does not have sufficient balance");

        sampleAccount = AccountRecord.create(
            1L, "111122223333", "Test Acc", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC1", "NY", "Bank", "1234"
        );
    }

    @Test
    @DisplayName("execute: Passes when account balance is sufficient")
    void execute_Pass() {
        PaymentRecord payment = PaymentRecord.create(null, "111122223333", "444455556666", BigDecimal.valueOf(500), "USD");

        when(accountService.getAccountByAccountNumber("111122223333")).thenReturn(Optional.of(sampleAccount));

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("execute: Fails when account balance is lower than payment amount")
    void execute_InsufficientBalance() {
        PaymentRecord payment = PaymentRecord.create(null, "111122223333", "444455556666", BigDecimal.valueOf(1500), "USD");

        when(accountService.getAccountByAccountNumber("111122223333")).thenReturn(Optional.of(sampleAccount));

        ValidationRuleResult result = rule.execute(payment, ruleDefinition);

        assertThat(result.passed()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
