package com.neueda.fraud;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;

/**
 * Fraud Decision Engine
 * Combines rule-based scores with ML probability to produce final fraud decision
 */
@Component
public class FraudDecisionEngine {
    
    @Value("${fraud.rule-weight:0.4}")
    private double ruleWeight;
    
    @Value("${fraud.ml-weight:0.6}")
    private double mlWeight;
    
    @Value("${fraud.review-threshold:0.75}")
    private double reviewThreshold;
    
    @Value("${fraud.auto-reject-threshold:0.95}")
    private double autoRejectThreshold;

    /**
     * Make final fraud decision based on hybrid scoring
     * 
     * @param ruleScore aggregated rule engine score (0-100)
     * @param mlProbability ML model fraud probability (0-1)
     * @return fraud decision with risk level
     */
    public FraudDecisionResult makeDecision(
        BigDecimal ruleScore,
        BigDecimal mlProbability
    ) {
        // Normalize inputs to 0-1 scale
        double normalizedRuleScore = safeNormalize(ruleScore);
        double normalizedMlScore = safeValue(mlProbability);
        
        // Calculate hybrid score (0-1 scale)
        double hybridScore = (normalizedRuleScore * ruleWeight) + (normalizedMlScore * mlWeight);
        
        // Convert to 0-100 scale for external representation
        BigDecimal hybridScore100 = BigDecimal.valueOf(hybridScore * 100)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Determine decision and risk level
        FraudDecision decision;
        FraudRiskLevel riskLevel;
        String explanation;
        
        if (hybridScore >= autoRejectThreshold) {
            decision = FraudDecision.REJECTED;
            riskLevel = FraudRiskLevel.CRITICAL;
            explanation = String.format(
                "CRITICAL FRAUD RISK: Hybrid score %.2f%% exceeds auto-reject threshold %.2f",
                hybridScore * 100, autoRejectThreshold * 100
            );
        } else if (hybridScore >= reviewThreshold) {
            decision = FraudDecision.SUSPICIOUS;
            riskLevel = FraudRiskLevel.HIGH;
            explanation = String.format(
                "High fraud risk detected. Hybrid score %.2f%% requires manual review. " +
                "Rule-based: %.2f%%, ML-based: %.2f%%",
                hybridScore * 100,
                normalizedRuleScore * 100,
                normalizedMlScore * 100
            );
        } else {
            decision = FraudDecision.APPROVED;
            riskLevel = FraudRiskLevel.fromScore(hybridScore100);
            explanation = String.format(
                "Payment approved. Hybrid score %.2f%% is within acceptable range. " +
                "Risk level: %s",
                hybridScore * 100, riskLevel.getDisplayName()
            );
        }
        
        return new FraudDecisionResult(
            decision,
            riskLevel,
            hybridScore100,
            BigDecimal.valueOf(normalizedRuleScore).multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP),
            BigDecimal.valueOf(normalizedMlScore).multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP),
            explanation
        );
    }
    
    /**
     * Normalize score from 0-100 scale to 0-1 scale
     */
    private double safeNormalize(BigDecimal score) {
        if (score == null) {
            return 0;
        }
        double value = score.doubleValue();
        // Clamp to 0-100 range, then normalize to 0-1
        value = Math.min(100, Math.max(0, value));
        return value / 100.0;
    }
    
    /**
     * Safely get value, defaulting to 0 if null
     */
    private double safeValue(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        double d = value.doubleValue();
        // Clamp to 0-1 range
        return Math.min(1.0, Math.max(0.0, d));
    }

    /**
     * Final fraud decision with supporting metrics
     */
    public record FraudDecisionResult(
        FraudDecision decision,
        FraudRiskLevel riskLevel,
        BigDecimal hybridScore,
        BigDecimal ruleEngineScore,
        BigDecimal mlScore,
        String explanation
    ) {
        public boolean isAutoApproved() {
            return decision == FraudDecision.APPROVED;
        }
        
        public boolean requiresManualReview() {
            return decision == FraudDecision.SUSPICIOUS;
        }
        
        public boolean isAutoRejected() {
            return decision == FraudDecision.REJECTED;
        }
    }
}

