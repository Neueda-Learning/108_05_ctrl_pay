package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

class UnusualTimePatternRuleTest {

    private UnusualTimePatternRule rule;
    private AccountRecord src;
    private AccountRecord dst;

    @BeforeEach
    void setUp() {
        rule = new UnusualTimePatternRule();
        src = new AccountRecord(1L, 10L, "ACC-01", "Test Acc 1", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
        dst = new AccountRecord(2L, 11L, "ACC-02", "Test Acc 2", new BigDecimal("1000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("evaluate: Off-hours on Weekend triggers score 30")
    void evaluate_OffHoursWeekend() {
        LocalDateTime offHoursWeekend = LocalDateTime.of(2026, 8, 8, 3, 0);
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, offHoursWeekend, offHoursWeekend);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(30, result.score());
    }

    @Test
    @DisplayName("evaluate: Off-hours on Weekday triggers score 15")
    void evaluate_OffHoursWeekday() {
        LocalDateTime offHoursWeekday = LocalDateTime.of(2026, 8, 7, 3, 0);
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, offHoursWeekday, offHoursWeekday);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(15, result.score());
    }

    @Test
    @DisplayName("evaluate: Weekend business hours triggers score 10")
    void evaluate_WeekendBusinessHours() {
        LocalDateTime weekendDay = LocalDateTime.of(2026, 8, 8, 14, 0);
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, weekendDay, weekendDay);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertTrue(result.triggered());
        assertEquals(10, result.score());
    }

    @Test
    @DisplayName("evaluate: Weekday business hours does not trigger")
    void evaluate_NormalBusinessHours() {
        LocalDateTime normal = LocalDateTime.of(2026, 8, 7, 14, 0);
        PaymentRecord payment = new PaymentRecord(1L, null, "ACC-01", "ACC-02", new BigDecimal("100"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, normal, normal);

        FraudRuleResult result = rule.evaluate(payment, src, dst);

        assertFalse(result.triggered());
    }

    @Test
    @DisplayName("Metadata checks")
    void metadata() {
        assertEquals("UNUSUAL_TIME_PATTERN", rule.getRuleName());
        assertNotNull(rule.getDescription());
        assertEquals(0.05, rule.getWeight());
    }
}
