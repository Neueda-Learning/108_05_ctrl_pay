package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fraud Rule Engine
 * Executes all registered fraud rules and aggregates their results
 */
@Component
public class FraudRuleEngine {
    
    private final List<FraudRule> rules;
    private final ObjectMapper objectMapper;

    public FraudRuleEngine(
        LargeTransactionRule largeTransactionRule,
        ExtremelyLargeTransactionRule extremelyLargeRule,
        AccountDrainRule accountDrainRule,
        TransactionVelocityRule velocityRule,
        BehaviorChangeRule behaviorChangeRule,
        NewDestinationRule newDestinationRule,
        MultipleFailureRule multipleFailureRule,
        SuspiciousAccountRule suspiciousAccountRule,
        CrossCurrencyRule crossCurrencyRule,
        MLFraudRule mlFraudRule,
        // NEW RULES - Phase 1 enhancements
        UnusualTimePatternRule unusualTimePatternRule,
        VelocityAnomalyRule velocityAnomalyRule,
        CyclicalTransactionPatternRule cyclicalPatternRule,
        BehavioralBaselineRule behavioralBaselineRule,
        ContextualRiskAggregationRule contextualRiskRule,
        ObjectMapper objectMapper
    ) {
        this.rules = List.of(
            largeTransactionRule,
            extremelyLargeRule,
            accountDrainRule,
            velocityRule,
            behaviorChangeRule,
            newDestinationRule,
            multipleFailureRule,
            suspiciousAccountRule,
            crossCurrencyRule,
            mlFraudRule,
            // NEW RULES
            unusualTimePatternRule,
            velocityAnomalyRule,
            cyclicalPatternRule,
            behavioralBaselineRule,
            contextualRiskRule
        );
        this.objectMapper = objectMapper;
    }

    /**
     * Execute all fraud rules against a payment
     * 
     * @param payment the payment to evaluate
     * @param sourceAccount the source account
     * @param destinationAccount the destination account
     * @return comprehensive fraud detection results
     */
    public FraudDetectionResult evaluatePayment(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        List<FraudRuleResult> allResults = new ArrayList<>();
        double totalWeight = 0;
        double weightedScoreSum = 0;
        List<String> triggeredRuleNames = new ArrayList<>();
        Map<String, RuleScoreDetail> ruleScores = new HashMap<>();
        
        // Execute each rule
        for (FraudRule rule : rules) {
            try {
                FraudRuleResult result = rule.evaluate(payment, sourceAccount, destinationAccount);
                allResults.add(result);
                
                double weight = rule.getWeight();
                totalWeight += weight;
                weightedScoreSum += result.score() * weight;
                
                ruleScores.put(rule.getRuleName(), new RuleScoreDetail(
                    rule.getRuleName(),
                    result.score(),
                    result.triggered(),
                    result.explanation(),
                    weight
                ));
                
                if (result.triggered()) {
                    triggeredRuleNames.add(rule.getRuleName());
                }
            } catch (Exception e) {
                // Individual rule failure should not crash the entire engine
                ruleScores.put(rule.getRuleName(), new RuleScoreDetail(
                    rule.getRuleName(),
                    0,
                    false,
                    "Rule execution error: " + e.getMessage(),
                    rule.getWeight()
                ));
            }
        }
        
        // Calculate final rule score (0-100)
        double finalRuleScore = totalWeight > 0 ? (weightedScoreSum / totalWeight) : 0;
        
        // Build explanation
        String explanation = buildExplanation(triggeredRuleNames, ruleScores);
        
        return new FraudDetectionResult(
            BigDecimal.valueOf(Math.min(100, Math.max(0, finalRuleScore))),
            allResults,
            triggeredRuleNames,
            ruleScores,
            explanation
        );
    }
    
    /**
     * Build human-readable explanation of fraud detection result
     */
    private String buildExplanation(
        List<String> triggeredRules,
        Map<String, RuleScoreDetail> ruleScores
    ) {
        if (triggeredRules.isEmpty()) {
            return "No fraud rules triggered. Payment appears legitimate.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Fraud rules triggered: ");
        sb.append(String.join(", ", triggeredRules));
        sb.append(". Details: ");
        
        List<String> details = triggeredRules.stream()
            .map(ruleName -> ruleScores.get(ruleName))
            .filter(detail -> detail != null)
            .map(detail -> String.format("%s (score: %d)", detail.ruleName, detail.score))
            .collect(Collectors.toList());
        
        sb.append(String.join("; ", details));
        
        return sb.toString();
    }

    /**
     * Get all registered fraud rules
     */
    public List<FraudRule> getRules() {
        return new ArrayList<>(rules);
    }
    
    /**
     * Detailed result of fraud detection across all rules
     */
    public record FraudDetectionResult(
        BigDecimal ruleEngineScore,
        List<FraudRuleResult> allRuleResults,
        List<String> triggeredRuleNames,
        Map<String, RuleScoreDetail> ruleScoresBreakdown,
        String explanation
    ) {}
    
    /**
     * Detailed breakdown of a single rule's score contribution
     */
    public record RuleScoreDetail(
        String ruleName,
        int score,
        boolean triggered,
        String explanation,
        double weight
    ) {}
}

