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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.FraudRuleRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.FraudRuleRepository;

@ExtendWith(MockitoExtension.class)
class FraudRuleEngineTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;
    @Mock
    private FraudRuleRegistry ruleRegistry;
    @Mock
    private FraudRule ruleImpl;

    private ObjectMapper objectMapper;
    private FraudRuleEngine engine;

    private PaymentRecord payment;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;
    private FraudRuleRecord ruleRecord;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        engine = new FraudRuleEngine(fraudRuleRepository, ruleRegistry, objectMapper);

        payment = new PaymentRecord(
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

        ruleRecord = FraudRuleRecord.create(
            "LARGE_TX_RULE", "AMOUNT", "Large TX", "HIGH", 1, BigDecimal.ONE, "{}", "{}"
        );
    }

    @Test
    @DisplayName("evaluatePayment: Executes active fraud rules and computes weighted score")
    void evaluatePayment_Success() {
        // Arrange
        when(fraudRuleRepository.findAllActive()).thenReturn(List.of(ruleRecord));
        when(ruleRegistry.getRule("LARGE_TX_RULE")).thenReturn(ruleImpl);
        
        FraudRuleResult ruleResult = FraudRuleResult.triggered("LARGE_TX_RULE", 50, "Triggered");
        when(ruleImpl.evaluate(payment, sourceAccount, destAccount)).thenReturn(ruleResult);

        // Act
        FraudRuleEngine.FraudDetectionResult result = engine.evaluatePayment(payment, sourceAccount, destAccount);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(50.0), result.ruleEngineScore());
        assertTrue(result.triggeredRuleNames().contains("LARGE_TX_RULE"));
    }

    @Test
    @DisplayName("evaluatePayment: Handles missing rule implementation gracefully")
    void evaluatePayment_MissingImplementation() {
        // Arrange
        when(fraudRuleRepository.findAllActive()).thenReturn(List.of(ruleRecord));
        when(ruleRegistry.getRule("LARGE_TX_RULE")).thenReturn(null);

        // Act
        FraudRuleEngine.FraudDetectionResult result = engine.evaluatePayment(payment, sourceAccount, destAccount);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(0.0), result.ruleEngineScore());
        assertFalse(result.triggeredRuleNames().contains("LARGE_TX_RULE"));
    }
}
