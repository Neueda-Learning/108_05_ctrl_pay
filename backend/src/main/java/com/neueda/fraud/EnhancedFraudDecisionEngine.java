package com.neueda.fraud;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.ProcessingLane;

/**
 * Enhanced Fraud Decision Engine
 * Combines rule-based scores with ML probability to produce final fraud decision
 * Supports dynamic thresholds, processing lanes, and confidence scoring
 */
@Component
public class EnhancedFraudDecisionEngine {
    
    // Hybrid scoring weights
    @Value("${fraud.rule-weight:0.4}")
    private double ruleWeight;
    
    @Value("${fraud.ml-weight:0.6}")
    private double mlWeight;
    
    // Decision thresholds (configurable)
    @Value("${fraud.approval-threshold:0.30}")
    private double approvalThreshold;
    
    @Value("${fraud.review-threshold:0.75}")
    private double reviewThreshold;
    
    @Value("${fraud.auto-reject-threshold:0.95}")
    private double autoRejectThreshold;
    
    // Risk-based routing SLAs (in minutes)
    @Value("${fraud.sla-fast-track:5}")
    private long slaFastTrack;
    
    @Value("${fraud.sla-manual-review:60}")
    private long slaManualReview;
    
    @Value("${fraud.sla-escalation:120}")
    private long slaEscalation;

    /**
     * Make final fraud decision based on hybrid scoring with enhanced metadata
     * 
     * @param ruleScore aggregated rule engine score (0-100)
     * @param mlProbability ML model fraud probability (0-1)
     * @param modelVersion which ML model made this prediction
     * @return enhanced fraud decision with risk level and processing lane
     */
    public EnhancedFraudDecisionResult makeDecision(
        BigDecimal ruleScore,
        BigDecimal mlProbability,
        String modelVersion
    ) {
        // Normalize inputs to 0-1 scale
        double normalizedRuleScore = safeNormalize(ruleScore);
        double normalizedMlScore = safeValue(mlProbability);
        
        // Calculate hybrid score (0-1 scale)
        double hybridScore = (normalizedRuleScore * ruleWeight) + (normalizedMlScore * mlWeight);
        
        // Convert to 0-100 scale for external representation
        BigDecimal hybridScore100 = BigDecimal.valueOf(hybridScore * 100)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Determine decision and risk level and processing lane
        FraudDecision decision;
        FraudRiskLevel riskLevel;
        ProcessingLane processingLane;
        long slaMinutes;
        String explanation;
        double confidence;
        
        if (hybridScore >= autoRejectThreshold) {
            // Critical fraud risk - auto-reject
            decision = FraudDecision.REJECTED;
            riskLevel = FraudRiskLevel.CRITICAL;
            processingLane = ProcessingLane.REJECTION;
            slaMinutes = 0;
            confidence = calculateConfidence(hybridScore, FraudRiskLevel.CRITICAL);
            explanation = String.format(
                "🚨 CRITICAL FRAUD RISK: Hybrid score %.2f%% exceeds auto-reject threshold %.2f%%. " +
                "Rule-based: %.2f%% | ML-based: %.2f%% | Model: %s | Decision: AUTO-REJECT",
                hybridScore * 100, 
                autoRejectThreshold * 100,
                normalizedRuleScore * 100,
                normalizedMlScore * 100,
                modelVersion
            );
            
        } else if (hybridScore >= reviewThreshold) {
            // High fraud risk - requires manual review
            decision = FraudDecision.SUSPICIOUS;
            riskLevel = FraudRiskLevel.HIGH;
            processingLane = ProcessingLane.MANUAL_REVIEW;
            slaMinutes = slaManualReview;
            confidence = calculateConfidence(hybridScore, FraudRiskLevel.HIGH);
            explanation = String.format(
                "⚠️ HIGH FRAUD RISK: Hybrid score %.2f%% exceeds review threshold %.2f%%. " +
                "Manual review required. Rule-based: %.2f%% | ML-based: %.2f%% | Model: %s | SLA: %d min",
                hybridScore * 100,
                reviewThreshold * 100,
                normalizedRuleScore * 100,
                normalizedMlScore * 100,
                modelVersion,
                slaMinutes
            );
            
        } else if (hybridScore >= approvalThreshold) {
            // Medium risk - escalation for complex cases
            decision = FraudDecision.APPROVED;
            riskLevel = FraudRiskLevel.MEDIUM;
            processingLane = ProcessingLane.ESCALATION;
            slaMinutes = slaEscalation;
            confidence = calculateConfidence(hybridScore, FraudRiskLevel.MEDIUM);
            explanation = String.format(
                "✓ MEDIUM RISK: Hybrid score %.2f%%. Approved but escalated for verification. " +
                "Rule-based: %.2f%% | ML-based: %.2f%% | Model: %s | SLA: %d min",
                hybridScore * 100,
                normalizedRuleScore * 100,
                normalizedMlScore * 100,
                modelVersion,
                slaMinutes
            );
            
        } else {
            // Low risk - auto-approve with fast track
            decision = FraudDecision.APPROVED;
            riskLevel = FraudRiskLevel.LOW;
            processingLane = ProcessingLane.FAST_TRACK;
            slaMinutes = slaFastTrack;
            confidence = calculateConfidence(hybridScore, FraudRiskLevel.LOW);
            explanation = String.format(
                "✅ LOW RISK: Hybrid score %.2f%% is within acceptable range. " +
                "Payment approved for fast-track processing. Rule-based: %.2f%% | ML-based: %.2f%% | Model: %s | SLA: %d min",
                hybridScore * 100,
                normalizedRuleScore * 100,
                normalizedMlScore * 100,
                modelVersion,
                slaMinutes
            );
        }
        
        return new EnhancedFraudDecisionResult(
            decision,
            riskLevel,
            hybridScore100,
            BigDecimal.valueOf(normalizedRuleScore).multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP),
            BigDecimal.valueOf(normalizedMlScore).multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP),
            explanation,
            processingLane,
            slaMinutes,
            BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP),
            modelVersion
        );
    }
    
    /**
     * Calculate overall confidence in the decision (0-1 scale)
     * Higher confidence when rule and ML scores agree
     */
    private double calculateConfidence(double hybridScore, FraudRiskLevel riskLevel) {
        // Confidence based on score distance from thresholds
        // Decisions at threshold boundaries have lower confidence
        
        double distanceFromThreshold;
        
        if (hybridScore < approvalThreshold) {
            // Low risk zone
            distanceFromThreshold = Math.abs(hybridScore - approvalThreshold);
            return Math.min(0.99, 0.85 + (Math.min(distanceFromThreshold / approvalThreshold, 0.14)));
        } else if (hybridScore < reviewThreshold) {
            // Medium risk zone
            distanceFromThreshold = Math.min(
                Math.abs(hybridScore - approvalThreshold),
                Math.abs(hybridScore - reviewThreshold)
            );
            return 0.75 + (Math.min(distanceFromThreshold / (reviewThreshold - approvalThreshold), 0.15));
        } else if (hybridScore < autoRejectThreshold) {
            // High risk zone
            distanceFromThreshold = Math.min(
                Math.abs(hybridScore - reviewThreshold),
                Math.abs(hybridScore - autoRejectThreshold)
            );
            return 0.80 + (Math.min(distanceFromThreshold / (autoRejectThreshold - reviewThreshold), 0.15));
        } else {
            // Critical risk zone
            distanceFromThreshold = Math.abs(hybridScore - autoRejectThreshold);
            return Math.min(0.99, 0.90 + (Math.min(distanceFromThreshold / (1.0 - autoRejectThreshold), 0.09)));
        }
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
     * Enhanced fraud decision with processing lanes and confidence
     */
    public record EnhancedFraudDecisionResult(
        FraudDecision decision,
        FraudRiskLevel riskLevel,
        BigDecimal hybridScore,
        BigDecimal ruleEngineScore,
        BigDecimal mlScore,
        String explanation,
        ProcessingLane processingLane,
        long slaMinutes,
        BigDecimal confidence,
        String mlModelVersion
    ) {
        public boolean isAutoApproved() {
            return decision == FraudDecision.APPROVED && processingLane == ProcessingLane.FAST_TRACK;
        }
        
        public boolean requiresManualReview() {
            return decision == FraudDecision.SUSPICIOUS;
        }
        
        public boolean isAutoRejected() {
            return decision == FraudDecision.REJECTED;
        }
        
        public boolean isEscalated() {
            return processingLane == ProcessingLane.ESCALATION;
        }
    }
}

