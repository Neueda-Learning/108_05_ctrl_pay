package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.neueda.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class TransactionVelocityRuleTest {

    @Mock
    private PaymentRepository paymentRepository;

    private TransactionVelocityRule rule;
    private PaymentRecord payment;
    private AccountRecord src;
    private AccountRecord dst;

    @BeforeEach
    void setUp() {
        rule = new TransactionVelocityRule(paymentRepository);
        ReflectionTestUtils.setField(rule, "maxTransactionsIn5Min", 5);
        ReflectionTestUtils.setField(rule, "maxTransactionsIn24h", 20);
        ReflectionTestUtils.setField(rule, "weight", 0.12);

        payment = new PaymentRecord(100L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        src = new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
        dst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Exceeding 5-minute velocity limit triggers score 40")
    void evaluate_Exceed5MinLimit() {
        List<PaymentRecord> list = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            list.add(new PaymentRecord(i, null, "ACC-01", "ACC-02", new BigDecimal("10"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusMinutes(2), LocalDateTime.now().minusMinutes(2)));
        }
        when(paymentRepository.findAll()).thenReturn(list);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(40, result.score());
    }

    @Test
    @DisplayName("evaluate: Exceeding 24-hour velocity limit triggers score 25")
    void evaluate_Exceed24hLimit() {
        List<PaymentRecord> list = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            list.add(new PaymentRecord(i, null, "ACC-01", "ACC-02", new BigDecimal("10"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(3)));
        }
        when(paymentRepository.findAll()).thenReturn(list);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(25, result.score());
    }

    @Test
    @DisplayName("evaluate: Normal transaction velocity does not trigger")
    void evaluate_NormalVelocity() {
        when(paymentRepository.findAll()).thenReturn(List.of());

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("TRANSACTION_VELOCITY", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.12, rule.getWeight());
    }
}
