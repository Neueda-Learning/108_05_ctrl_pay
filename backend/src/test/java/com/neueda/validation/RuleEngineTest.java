package com.neueda.validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.RuleType;
import com.neueda.domain.Severity;
import com.neueda.domain.ValidationResultRecord;
import com.neueda.domain.ValidationRuleRecord;
import com.neueda.validation.rules.ValidationRule;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private RuleFactory ruleFactory;

    @Mock
    private ValidationRule validationRule;

    private RuleEngine ruleEngine;
    private ObjectMapper objectMapper;
    private ObjectNode def;

    private PaymentRecord payment;
    private ValidationRuleRecord hardRule;
    private ValidationRuleRecord softRule;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine(ruleFactory);
        objectMapper = new ObjectMapper();
        def = objectMapper.createObjectNode();

        payment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE, PaymentStatus.CREATED,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        hardRule = new ValidationRuleRecord(1L, "DIFF_ACC", "Account Diff", RuleType.ACCOUNT_DIFFERENCE, def, true, Severity.HARD, 1, LocalDateTime.now(), LocalDateTime.now());
        softRule = new ValidationRuleRecord(2L, "AMT_RANGE", "Amount Range", RuleType.AMOUNT_RANGE, def, true, Severity.SOFT, 2, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("validatePayment: Executes active rules and records results")
    void validatePayment_Success() {
        when(ruleFactory.createRule(RuleType.ACCOUNT_DIFFERENCE)).thenReturn(validationRule);
        when(validationRule.execute(any(), any())).thenReturn(ValidationRule.ValidationRuleResult.success(10L));

        List<ValidationResultRecord> results = ruleEngine.validatePayment(payment, List.of(hardRule));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).passed()).isTrue();
        assertThat(results.get(0).ruleName()).isEqualTo("DIFF_ACC");
    }

    @Test
    @DisplayName("hasPassedValidation: HARD rule failure causes validation to fail")
    void hasPassedValidation_HardRuleFailed() {
        ValidationResultRecord hardFailed = new ValidationResultRecord(1L, 1L, 1L, "DIFF_ACC", def, false, "ERR", "Failed", 10, LocalDateTime.now());
        ValidationResultRecord softPassed = new ValidationResultRecord(2L, 1L, 2L, "AMT_RANGE", def, true, null, null, 5, LocalDateTime.now());

        boolean passed = ruleEngine.hasPassedValidation(List.of(hardFailed, softPassed), List.of(hardRule, softRule));

        assertThat(passed).isFalse();
    }

    @Test
    @DisplayName("hasPassedValidation: Only SOFT rule failure allows validation to pass")
    void hasPassedValidation_SoftRuleFailed() {
        ValidationResultRecord hardPassed = new ValidationResultRecord(1L, 1L, 1L, "DIFF_ACC", def, true, null, null, 10, LocalDateTime.now());
        ValidationResultRecord softFailed = new ValidationResultRecord(2L, 1L, 2L, "AMT_RANGE", def, false, "WARN", "Soft limit warning", 5, LocalDateTime.now());

        boolean passed = ruleEngine.hasPassedValidation(List.of(hardPassed, softFailed), List.of(hardRule, softRule));

        assertThat(passed).isTrue();
    }

    @Test
    @DisplayName("aggregateErrorMessage: Aggregates error messages from failed rules")
    void aggregateErrorMessage() {
        ValidationResultRecord r1 = new ValidationResultRecord(1L, 1L, 1L, "Rule1", def, false, "E1", "Error 1", 5, LocalDateTime.now());
        ValidationResultRecord r2 = new ValidationResultRecord(2L, 1L, 2L, "Rule2", def, false, "E2", "Error 2", 5, LocalDateTime.now());

        String aggregated = ruleEngine.aggregateErrorMessage(List.of(r1, r2));

        assertThat(aggregated).isEqualTo("Rule1: Error 1; Rule2: Error 2");
    }
}
