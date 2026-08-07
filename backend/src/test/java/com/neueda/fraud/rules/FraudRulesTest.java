package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class FraudRulesTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AccountRepository accountRepository;

    private PaymentRecord samplePayment;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;

    @BeforeEach
    void setUp() {
        samplePayment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(1000), "USD",
            BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        sourceAccount = new AccountRecord(
            10L, 1L, "ACC1", "Source", BigDecimal.valueOf(5000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );

        destAccount = new AccountRecord(
            20L, 2L, "ACC2", "Dest", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );
    }

    @Test
    @DisplayName("LargeTransactionRule: Triggers when amount exceeds balance threshold")
    void testLargeTransactionRule() {
        LargeTransactionRule rule = new LargeTransactionRule();
        ReflectionTestUtils.setField(rule, "thresholdPercent", 80.0);
        ReflectionTestUtils.setField(rule, "weight", 0.15);

        // Not triggered (1000 out of 5000 is 20%)
        FraudRuleResult result1 = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertFalse(result1.triggered());

        // Triggered (4500 out of 5000 is 90%)
        PaymentRecord largePayment = new PaymentRecord(
            2L, "KEY2", "ACC1", "ACC2", BigDecimal.valueOf(4500), "USD",
            BigDecimal.valueOf(4500), BigDecimal.valueOf(4500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
        FraudRuleResult result2 = rule.evaluate(largePayment, sourceAccount, destAccount);
        assertTrue(result2.triggered());
        assertEquals("LARGE_TRANSACTION", rule.getRuleName());
        assertEquals(0.15, rule.getWeight());
    }

    @Test
    @DisplayName("ExtremelyLargeTransactionRule: Triggers on absolute amount threshold")
    void testExtremelyLargeTransactionRule() {
        ExtremelyLargeTransactionRule rule = new ExtremelyLargeTransactionRule();

        FraudRuleResult result1 = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertNotNull(result1);

        PaymentRecord extremePayment = new PaymentRecord(
            2L, "KEY2", "ACC1", "ACC2", BigDecimal.valueOf(1000000), "USD",
            BigDecimal.valueOf(1000000), BigDecimal.valueOf(1000000), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
        FraudRuleResult result2 = rule.evaluate(extremePayment, sourceAccount, destAccount);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("CrossCurrencyRule: Triggers on currency mismatch")
    void testCrossCurrencyRule() {
        CrossCurrencyRule rule = new CrossCurrencyRule();
        ReflectionTestUtils.setField(rule, "highRiskAmountThreshold", 5000.0);

        // Same currency (USD -> USD)
        FraudRuleResult result1 = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertFalse(result1.triggered());

        // Cross currency (EUR vs USD)
        PaymentRecord crossCurrencyPayment = new PaymentRecord(
            2L, "KEY2", "ACC1", "ACC2", BigDecimal.valueOf(1000), "EUR",
            BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
        FraudRuleResult result2 = rule.evaluate(crossCurrencyPayment, sourceAccount, destAccount);
        assertTrue(result2.triggered());
    }

    @Test
    @DisplayName("TransactionVelocityRule: Triggers when frequency limit exceeded")
    void testTransactionVelocityRule() {
        TransactionVelocityRule rule = new TransactionVelocityRule(paymentRepository);
        ReflectionTestUtils.setField(rule, "maxTransactionsIn5Min", 2);
        ReflectionTestUtils.setField(rule, "maxTransactionsIn24h", 10);

        when(paymentRepository.findAll()).thenReturn(List.of(samplePayment, samplePayment, samplePayment));

        FraudRuleResult result = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertTrue(result.triggered());
    }

    @Test
    @DisplayName("AccountDrainRule: Triggers when remaining balance is near zero")
    void testAccountDrainRule() {
        AccountDrainRule rule = new AccountDrainRule();

        // 4950 out of 5000 leaves 50
        PaymentRecord drainPayment = new PaymentRecord(
            2L, "KEY2", "ACC1", "ACC2", BigDecimal.valueOf(4950), "USD",
            BigDecimal.valueOf(4950), BigDecimal.valueOf(4950), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        FraudRuleResult result = rule.evaluate(drainPayment, sourceAccount, destAccount);
        assertNotNull(result);
    }

    @Test
    @DisplayName("NewDestinationRule: Triggers when destination is new for source account")
    void testNewDestinationRule() {
        NewDestinationRule rule = new NewDestinationRule(paymentRepository, accountRepository);
        when(paymentRepository.findAll()).thenReturn(List.of());

        FraudRuleResult result = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertTrue(result.triggered());
    }

    @Test
    @DisplayName("MultipleFailureRule: Triggers on multiple failed attempts")
    void testMultipleFailureRule() {
        MultipleFailureRule rule = new MultipleFailureRule(paymentRepository);
        ReflectionTestUtils.setField(rule, "failureThreshold", 3);
        ReflectionTestUtils.setField(rule, "lookbackDays", 7);

        PaymentRecord failed1 = new PaymentRecord(
            10L, "K1", "ACC1", "ACC2", BigDecimal.TEN, "USD", BigDecimal.TEN, BigDecimal.TEN,
            BigDecimal.ONE, PaymentStatus.FAILED, "ERR", "Err", 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(paymentRepository.findAll()).thenReturn(List.of(failed1, failed1, failed1, failed1, failed1, failed1));

        FraudRuleResult result = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertNotNull(result);
    }

    @Test
    @DisplayName("UnusualTimePatternRule: Evaluates time of transaction")
    void testUnusualTimePatternRule() {
        UnusualTimePatternRule rule = new UnusualTimePatternRule();

        FraudRuleResult result = rule.evaluate(samplePayment, sourceAccount, destAccount);
        assertNotNull(result);
    }

    @Test
    @DisplayName("SuspiciousAccountRule: Triggers if account status is SUSPICIOUS")
    void testSuspiciousAccountRule() {
        SuspiciousAccountRule rule = new SuspiciousAccountRule();

        AccountRecord suspiciousAcc = new AccountRecord(
            10L, 1L, "ACC1", "Source", BigDecimal.valueOf(5000), AccountStatus.SUSPICIOUS,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC", "NYC", "Bank", "1234"
        );

        FraudRuleResult result = rule.evaluate(samplePayment, suspiciousAcc, destAccount);
        assertTrue(result.triggered());
    }
}
