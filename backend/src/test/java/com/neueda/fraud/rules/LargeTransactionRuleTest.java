package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class LargeTransactionRuleTest {

    private LargeTransactionRule rule;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;
    private PaymentRecord payment;

    @BeforeEach
    void setUp() {
        rule = new LargeTransactionRule();
        ReflectionTestUtils.setField(rule, "thresholdPercent", 80.0);
        ReflectionTestUtils.setField(rule, "weight", 0.15);

        sourceAccount = new AccountRecord(
            1L, 101L, "111122223333", "John Doe Account", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "1234"
        );

        destAccount = new AccountRecord(
            2L, 102L, "444455556666", "Jane Smith Account", BigDecimal.valueOf(500), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "5678"
        );

        payment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(900), "USD",
            BigDecimal.valueOf(900), BigDecimal.valueOf(900), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("evaluate: Triggers when percentage > threshold")
    void evaluate_Triggers() {
        FraudRuleResult result = rule.evaluate(payment, sourceAccount, destAccount);

        assertTrue(result.triggered());
        assertEquals("LARGE_TRANSACTION", result.ruleName());
    }

    @Test
    @DisplayName("evaluate: Does not trigger when percentage <= threshold")
    void evaluate_NotTriggered() {
        PaymentRecord smallPayment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        FraudRuleResult result = rule.evaluate(smallPayment, sourceAccount, destAccount);

        assertFalse(result.triggered());
    }
}
