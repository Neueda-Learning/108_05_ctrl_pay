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
class CyclicalTransactionPatternRuleTest {

    @Mock
    private PaymentRepository paymentRepository;

    private CyclicalTransactionPatternRule rule;
    private PaymentRecord currentPayment;
    private AccountRecord srcAccount;
    private AccountRecord dstAccount;

    @BeforeEach
    void setUp() {
        rule = new CyclicalTransactionPatternRule(paymentRepository);
        ReflectionTestUtils.setField(rule, "lookbackHours", 24);
        ReflectionTestUtils.setField(rule, "weight", 0.08);

        currentPayment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("500"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        srcAccount = new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
        dstAccount = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Detects reverse transaction from ACC-02 to ACC-01")
    void evaluate_CyclicalFound() {
        PaymentRecord reversePayment = new PaymentRecord(2L, null, "ACC-02", "ACC-01", new BigDecimal("400"), "USD", null, null, null, PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));
        when(paymentRepository.findAll()).thenReturn(List.of(reversePayment));

        FraudRuleResult result = rule.evaluate(currentPayment, srcAccount, dstAccount);

        assertTrue(result.triggered());
        assertEquals(50, result.score());
    }

    @Test
    @DisplayName("evaluate: No cyclical pattern when no prior transaction exists")
    void evaluate_NoCyclical() {
        PaymentRecord unrelatedPayment = new PaymentRecord(2L, null, "ACC-01", "ACC-03", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));
        when(paymentRepository.findAll()).thenReturn(List.of(unrelatedPayment));

        FraudRuleResult result = rule.evaluate(currentPayment, srcAccount, dstAccount);

        assertFalse(result.triggered());
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("evaluate: Ignore failed prior reverse transactions")
    void evaluate_ReversePaymentFailed() {
        PaymentRecord failedReverse = new PaymentRecord(2L, null, "ACC-02", "ACC-01", new BigDecimal("400"), "USD", null, null, null, PaymentStatus.FAILED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));
        when(paymentRepository.findAll()).thenReturn(List.of(failedReverse));

        FraudRuleResult result = rule.evaluate(currentPayment, srcAccount, dstAccount);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("evaluate: Exception in repository returns not triggered")
    void evaluate_RepositoryException() {
        when(paymentRepository.findAll()).thenThrow(new RuntimeException("DB offline"));

        FraudRuleResult result = rule.evaluate(currentPayment, srcAccount, dstAccount);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("CYCLICAL_TRANSACTION_PATTERN", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.08, rule.getWeight());
    }
}
