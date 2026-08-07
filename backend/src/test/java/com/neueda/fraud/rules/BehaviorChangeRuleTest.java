package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.neueda.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class BehaviorChangeRuleTest {

    @Mock
    private PaymentRepository paymentRepository;

    private BehaviorChangeRule rule;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;
    private PaymentRecord payment;

    @BeforeEach
    void setUp() {
        rule = new BehaviorChangeRule(paymentRepository);
        ReflectionTestUtils.setField(rule, "deviationThreshold", 3.0);
        ReflectionTestUtils.setField(rule, "lookbackDays", 30);
        ReflectionTestUtils.setField(rule, "weight", 0.12);

        sourceAccount = new AccountRecord(
            1L, 101L, "111122223333", "John Doe Account", BigDecimal.valueOf(10000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "1234"
        );

        destAccount = new AccountRecord(
            2L, 102L, "444455556666", "Jane Smith Account", BigDecimal.valueOf(5000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "5678"
        );

        payment = new PaymentRecord(
            10L, "KEY10", "111122223333", "444455556666", BigDecimal.valueOf(10000), "USD",
            BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("evaluate: Not triggered when insufficient history")
    void evaluate_InsufficientHistory() {
        when(paymentRepository.findAll()).thenReturn(List.of());

        FraudRuleResult result = rule.evaluate(payment, sourceAccount, destAccount);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("evaluate: Triggers when current transaction is far above historical average")
    void evaluate_TriggersOnHighZScore() {
        PaymentRecord h1 = new PaymentRecord(1L, "K1", "111122223333", "444455556666", BigDecimal.valueOf(10), "USD", BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.ONE, PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        PaymentRecord h2 = new PaymentRecord(2L, "K2", "111122223333", "444455556666", BigDecimal.valueOf(12), "USD", BigDecimal.valueOf(12), BigDecimal.valueOf(12), BigDecimal.ONE, PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        PaymentRecord h3 = new PaymentRecord(3L, "K3", "111122223333", "444455556666", BigDecimal.valueOf(11), "USD", BigDecimal.valueOf(11), BigDecimal.valueOf(11), BigDecimal.ONE, PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());

        when(paymentRepository.findAll()).thenReturn(List.of(h1, h2, h3));

        FraudRuleResult result = rule.evaluate(payment, sourceAccount, destAccount);

        assertTrue(result.triggered());
    }
}
