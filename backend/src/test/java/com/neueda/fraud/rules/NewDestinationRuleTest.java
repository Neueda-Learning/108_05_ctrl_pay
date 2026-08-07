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
class NewDestinationRuleTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    private NewDestinationRule rule;
    private PaymentRecord payment;
    private AccountRecord src;

    @BeforeEach
    void setUp() {
        rule = new NewDestinationRule(paymentRepository, accountRepository);
        ReflectionTestUtils.setField(rule, "newAccountThresholdDays", 30);
        ReflectionTestUtils.setField(rule, "weight", 0.10);

        payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        src = new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now().minusDays(100), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Destination created recently triggers rule with score 15")
    void evaluate_NewDestinationAccount() {
        AccountRecord recentDst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now().minusDays(5), "IFSC0001234", "NY", "Bank", "1234");

        FraudRuleResult result = rule.evaluate(payment, src, recentDst);

        assertTrue(result.triggered());
        assertEquals(15, result.score());
    }

    @Test
    @DisplayName("evaluate: No prior transaction history triggers rule with score 12")
    void evaluate_NoPriorHistory() {
        AccountRecord oldDst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now().minusDays(50), "IFSC0001234", "NY", "Bank", "1234");
        when(paymentRepository.findAll()).thenReturn(List.of());

        FraudRuleResult result = rule.evaluate(payment, src, oldDst);

        assertTrue(result.triggered());
        assertEquals(12, result.score());
    }

    @Test
    @DisplayName("evaluate: Established recipient with prior completed transactions does not trigger")
    void evaluate_EstablishedRecipient() {
        AccountRecord oldDst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now().minusDays(50), "IFSC0001234", "NY", "Bank", "1234");
        PaymentRecord prior = new PaymentRecord(2L, null, "ACC-01", "ACC-02", new BigDecimal("50"), "USD", null, null, null, PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(10));
        when(paymentRepository.findAll()).thenReturn(List.of(prior));

        FraudRuleResult result = rule.evaluate(payment, src, oldDst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("NEW_DESTINATION", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.10, rule.getWeight());
    }
}
