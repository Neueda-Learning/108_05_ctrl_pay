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
import com.neueda.domain.ProcessingLane;

class EnhancedFraudDecisionEngineTest {

    private EnhancedFraudDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EnhancedFraudDecisionEngine();
        ReflectionTestUtils.setField(engine, "ruleWeight", 0.4);
        ReflectionTestUtils.setField(engine, "mlWeight", 0.6);
        ReflectionTestUtils.setField(engine, "approvalThreshold", 0.30);
        ReflectionTestUtils.setField(engine, "reviewThreshold", 0.75);
        ReflectionTestUtils.setField(engine, "autoRejectThreshold", 0.95);
        ReflectionTestUtils.setField(engine, "slaFastTrack", 5L);
        ReflectionTestUtils.setField(engine, "slaManualReview", 60L);
        ReflectionTestUtils.setField(engine, "slaEscalation", 120L);
    }

    @Test
    @DisplayName("makeDecision: Fast track approval for low risk")
    void makeDecision_LowRisk_FastTrack() {
        // Act
        EnhancedFraudDecisionEngine.EnhancedFraudDecisionResult result =
            engine.makeDecision(BigDecimal.valueOf(10), BigDecimal.valueOf(0.1), "v1.0");

        // Assert
        assertNotNull(result);
        assertEquals(FraudDecision.APPROVED, result.decision());
        assertEquals(FraudRiskLevel.LOW, result.riskLevel());
        assertEquals(ProcessingLane.FAST_TRACK, result.processingLane());
        assertTrue(result.isAutoApproved());
        assertEquals(5L, result.slaMinutes());
    }

    @Test
    @DisplayName("makeDecision: Escalation for medium risk")
    void makeDecision_MediumRisk_Escalation() {
        // Act (Hybrid = 0.5 * 0.4 + 0.5 * 0.6 = 0.5, which is between 0.30 and 0.75)
        EnhancedFraudDecisionEngine.EnhancedFraudDecisionResult result =
            engine.makeDecision(BigDecimal.valueOf(50), BigDecimal.valueOf(0.5), "v1.0");

        // Assert
        assertEquals(FraudDecision.APPROVED, result.decision());
        assertEquals(FraudRiskLevel.MEDIUM, result.riskLevel());
        assertEquals(ProcessingLane.ESCALATION, result.processingLane());
        assertTrue(result.isEscalated());
    }

    @Test
    @DisplayName("makeDecision: Manual review for high risk")
    void makeDecision_HighRisk_ManualReview() {
        // Act (Hybrid = 0.8 * 0.4 + 0.8 * 0.6 = 0.8 >= 0.75)
        EnhancedFraudDecisionEngine.EnhancedFraudDecisionResult result =
            engine.makeDecision(BigDecimal.valueOf(80), BigDecimal.valueOf(0.8), "v1.0");

        // Assert
        assertEquals(FraudDecision.SUSPICIOUS, result.decision());
        assertEquals(FraudRiskLevel.HIGH, result.riskLevel());
        assertEquals(ProcessingLane.MANUAL_REVIEW, result.processingLane());
        assertTrue(result.requiresManualReview());
    }

    @Test
    @DisplayName("makeDecision: Rejection for critical risk")
    void makeDecision_CriticalRisk_Rejection() {
        // Act (Hybrid = 1.0 * 0.4 + 0.96 * 0.6 = 0.976 >= 0.95)
        EnhancedFraudDecisionEngine.EnhancedFraudDecisionResult result =
            engine.makeDecision(BigDecimal.valueOf(100), BigDecimal.valueOf(0.96), "v1.0");

        // Assert
        assertEquals(FraudDecision.REJECTED, result.decision());
        assertEquals(FraudRiskLevel.CRITICAL, result.riskLevel());
        assertEquals(ProcessingLane.REJECTION, result.processingLane());
        assertTrue(result.isAutoRejected());
    }

    @Test
    @DisplayName("makeDecision: Null scores handled gracefully")
    void makeDecision_NullScores() {
        // Act
        EnhancedFraudDecisionEngine.EnhancedFraudDecisionResult result =
            engine.makeDecision(null, null, "v1.0");

        // Assert
        assertEquals(FraudDecision.APPROVED, result.decision());
        assertEquals(FraudRiskLevel.LOW, result.riskLevel());
    }
}
