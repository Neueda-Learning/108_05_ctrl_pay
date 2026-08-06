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
class CrossCurrencyRuleTest {

    private CrossCurrencyRule rule;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;

    @BeforeEach
    void setUp() {
        rule = new CrossCurrencyRule();
        ReflectionTestUtils.setField(rule, "highRiskAmountThreshold", 5000.0);
        ReflectionTestUtils.setField(rule, "weight", 0.08);

        sourceAccount = new AccountRecord(
            1L, 101L, "111122223333", "John Doe Account", BigDecimal.valueOf(10000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "1234"
        );

        destAccount = new AccountRecord(
            2L, 102L, "444455556666", "Jane Smith Account", BigDecimal.valueOf(5000), AccountStatus.ACTIVE,
            "EUR", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "5678"
        );
    }

    @Test
    @DisplayName("evaluate: Triggers high-risk for high-value cross-currency transfer")
    void evaluate_HighRiskCrossCurrency() {
        PaymentRecord crossPayment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(6000), "EUR",
            BigDecimal.valueOf(6000), BigDecimal.valueOf(5100), BigDecimal.valueOf(0.85),
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        FraudRuleResult result = rule.evaluate(crossPayment, sourceAccount, destAccount);

        assertTrue(result.triggered());
        assertEquals("CROSS_CURRENCY_RISK", result.ruleName());
    }

    @Test
    @DisplayName("evaluate: Does not trigger for domestic same-currency transfer")
    void evaluate_DomesticSameCurrency() {
        PaymentRecord samePayment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        FraudRuleResult result = rule.evaluate(samePayment, sourceAccount, destAccount);

        assertFalse(result.triggered());
    }
}
