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
import com.neueda.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class MultipleFailureRuleTest {

    @Mock
    private PaymentRepository paymentRepository;

    private MultipleFailureRule rule;
    private PaymentRecord payment;
    private AccountRecord src;
    private AccountRecord dst;

    @BeforeEach
    void setUp() {
        rule = new MultipleFailureRule(paymentRepository);
        ReflectionTestUtils.setField(rule, "failureThreshold", 3);
        ReflectionTestUtils.setField(rule, "lookbackDays", 7);
        ReflectionTestUtils.setField(rule, "weight", 0.10);

        payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        src = new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
        dst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Failed count >= threshold triggers rule")
    void evaluate_Triggered() {
        PaymentRecord f1 = new PaymentRecord(2L, null, "ACC-01", "ACC-03", new BigDecimal("50"), "USD", null, null, null, PaymentStatus.FAILED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        PaymentRecord f2 = new PaymentRecord(3L, null, "ACC-01", "ACC-04", new BigDecimal("50"), "USD", null, null, null, PaymentStatus.FAILED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2));
        PaymentRecord f3 = new PaymentRecord(4L, null, "ACC-01", "ACC-05", new BigDecimal("50"), "USD", null, null, null, PaymentStatus.FAILED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3));

        when(paymentRepository.findAll()).thenReturn(List.of(f1, f2, f3));

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(20, result.score());
    }

    @Test
    @DisplayName("evaluate: Failed count < threshold does not trigger rule")
    void evaluate_NotTriggered() {
        PaymentRecord f1 = new PaymentRecord(2L, null, "ACC-01", "ACC-03", new BigDecimal("50"), "USD", null, null, null, PaymentStatus.FAILED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        when(paymentRepository.findAll()).thenReturn(List.of(f1));

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("MULTIPLE_FAILURES", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.10, rule.getWeight());
    }
}
