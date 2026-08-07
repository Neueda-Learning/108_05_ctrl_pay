package com.neueda.fraud;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;

class FraudDecisionEngineTest {

    private FraudDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FraudDecisionEngine();
        ReflectionTestUtils.setField(engine, "ruleWeight", 0.4);
        ReflectionTestUtils.setField(engine, "mlWeight", 0.6);
        ReflectionTestUtils.setField(engine, "reviewThreshold", 0.75);
        ReflectionTestUtils.setField(engine, "autoRejectThreshold", 0.95);
    }

    @Test
    @DisplayName("makeDecision: Approved low risk when hybrid score is low")
    void makeDecision_Approved() {
        // Arrange
        BigDecimal ruleScore = BigDecimal.valueOf(10); // 0.1
        BigDecimal mlProbability = BigDecimal.valueOf(0.1); // 0.1
        // hybridScore = (0.1 * 0.4) + (0.1 * 0.6) = 0.1 (10%)

        // Act
        FraudDecisionEngine.FraudDecisionResult result = engine.makeDecision(ruleScore, mlProbability);

        // Assert
        assertNotNull(result);
        assertEquals(FraudDecision.APPROVED, result.decision());
        assertEquals(FraudRiskLevel.LOW, result.riskLevel());
        assertTrue(result.isAutoApproved());
        assertFalse(result.requiresManualReview());
        assertFalse(result.isAutoRejected());
    }

    @Test
    @DisplayName("makeDecision: Suspicious high risk when hybrid score >= review threshold")
    void makeDecision_Suspicious() {
        // Arrange
        BigDecimal ruleScore = BigDecimal.valueOf(80); // 0.8
        BigDecimal mlProbability = BigDecimal.valueOf(0.8); // 0.8
        // hybridScore = (0.8 * 0.4) + (0.8 * 0.6) = 0.8 (80%) >= 0.75

        // Act
        FraudDecisionEngine.FraudDecisionResult result = engine.makeDecision(ruleScore, mlProbability);

        // Assert
        assertEquals(FraudDecision.SUSPICIOUS, result.decision());
        assertEquals(FraudRiskLevel.HIGH, result.riskLevel());
        assertTrue(result.requiresManualReview());
    }

    @Test
    @DisplayName("makeDecision: Rejected critical risk when hybrid score >= auto reject threshold")
    void makeDecision_Rejected() {
        // Arrange
        BigDecimal ruleScore = BigDecimal.valueOf(100); // 1.0
        BigDecimal mlProbability = BigDecimal.valueOf(0.98); // 0.98
        // hybridScore = (1.0 * 0.4) + (0.98 * 0.6) = 0.988 (98.8%) >= 0.95

        // Act
        FraudDecisionEngine.FraudDecisionResult result = engine.makeDecision(ruleScore, mlProbability);

        // Assert
        assertEquals(FraudDecision.REJECTED, result.decision());
        assertEquals(FraudRiskLevel.CRITICAL, result.riskLevel());
        assertTrue(result.isAutoRejected());
    }

    @Test
    @DisplayName("makeDecision: Handles null inputs gracefully")
    void makeDecision_NullInputs() {
        // Arrange & Act
        FraudDecisionEngine.FraudDecisionResult result = engine.makeDecision(null, null);

        // Assert
        assertEquals(FraudDecision.APPROVED, result.decision());
        assertEquals(BigDecimal.ZERO.setScale(2), result.hybridScore());
    }
}
