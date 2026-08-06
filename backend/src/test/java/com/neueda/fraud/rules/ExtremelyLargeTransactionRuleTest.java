package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

class ExtremelyLargeTransactionRuleTest {

    private ExtremelyLargeTransactionRule rule;
    private PaymentRecord payment;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;

    @BeforeEach
    void setUp() {
        rule = new ExtremelyLargeTransactionRule();

        payment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(1000000), "USD",
            BigDecimal.valueOf(1000000), BigDecimal.valueOf(1000000), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        sourceAccount = new AccountRecord(
            10L, 1L, "ACC1", "Source", BigDecimal.valueOf(5000000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );

        destAccount = new AccountRecord(
            20L, 2L, "ACC2", "Dest", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );
    }

    @Test
    @DisplayName("evaluate: Evaluates extremely large transaction rule")
    void testEvaluate() {
        FraudRuleResult result = rule.evaluate(payment, sourceAccount, destAccount);
        assertNotNull(result);
        assertEquals("EXTREMELY_LARGE_TRANSACTION", rule.getRuleName());
        assertNotNull(rule.getDescription());
    }
}
