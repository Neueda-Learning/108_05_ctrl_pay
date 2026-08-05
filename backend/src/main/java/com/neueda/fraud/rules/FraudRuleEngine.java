package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.FraudRuleRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.repository.FraudRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fraud Rule Engine - Enhanced for Dynamic Rule Loading
 * 
 * Responsibilities:
 * 1. Load active fraud rules from database (not hardcoded)
 * 2. Execute each rule against a payment
 * 3. Apply rule weights and severity from database
 * 4. Aggregate results into comprehensive fraud score
 * 
 * Key improvement: Rules are now loaded dynamically from database,
 * allowing compliance/business users to modify rules without code redeployment.
 */
@Component
public class FraudRuleEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudRuleEngine.class);
    
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudRuleRegistry ruleRegistry;
    private final ObjectMapper objectMapper;

    public FraudRuleEngine(
        FraudRuleRepository fraudRuleRepository,
        FraudRuleRegistry ruleRegistry,
        ObjectMapper objectMapper
    ) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.ruleRegistry = ruleRegistry;
        this.objectMapper = objectMapper;
        logger.info("FraudRuleEngine initialized with dynamic rule loading. Registry size: {}", 
            ruleRegistry.getRegistrySize());
    }

    /**
     * Execute all active fraud rules against a payment
     * 
     * Process:
     * 1. Load active fraud rules from database (sorted by order_of_execution)
     * 2. For each rule:
     *    - Look up implementation via FraudRuleRegistry
     *    - Execute rule.evaluate()
     *    - Apply weight and severity from database
     * 3. Aggregate weighted results into final fraud score (0-100)
     * 4. Return comprehensive fraud detection results
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
        
        // Step 1: Load active fraud rules from database (ordered by execution sequence)
        List<FraudRuleRecord> activeRules = fraudRuleRepository.findAllActive();
        logger.debug("Evaluating payment {} with {} active fraud rules", payment.id(), activeRules.size());
        
        // Step 2: Execute each rule
        for (FraudRuleRecord ruleRecord : activeRules) {
            try {
                // Look up the rule implementation
                FraudRule ruleImplementation = ruleRegistry.getRule(ruleRecord.ruleName());
                
                if (ruleImplementation == null) {
                    logger.warn("Fraud rule implementation not found for rule: {}", ruleRecord.ruleName());
                    ruleScores.put(ruleRecord.ruleName(), new RuleScoreDetail(
                        ruleRecord.ruleName(),
                        0,
                        false,
                        "Rule implementation not registered: " + ruleRecord.ruleName(),
                        ruleRecord.weight().doubleValue()
                    ));
                    continue;
                }
                
                // Execute the rule
                FraudRuleResult result = ruleImplementation.evaluate(payment, sourceAccount, destinationAccount);
                allResults.add(result);
                
                // Apply weight from database (not from rule's hardcoded weight)
                double dbWeight = ruleRecord.weight().doubleValue();
                totalWeight += dbWeight;
                weightedScoreSum += result.score() * dbWeight;
                
                ruleScores.put(ruleRecord.ruleName(), new RuleScoreDetail(
                    ruleRecord.ruleName(),
                    result.score(),
                    result.triggered(),
                    result.explanation(),
                    dbWeight
                ));
                
                if (result.triggered()) {
                    triggeredRuleNames.add(ruleRecord.ruleName());
                    logger.debug("Fraud rule {} triggered for payment {} with score {}", 
                        ruleRecord.ruleName(), payment.id(), result.score());
                }
            } catch (Exception e) {
                logger.error("Error executing fraud rule {} for payment {}: {}", 
                    ruleRecord.ruleName(), payment.id(), e.getMessage(), e);
                
                // Individual rule failure should not crash the entire engine
                ruleScores.put(ruleRecord.ruleName(), new RuleScoreDetail(
                    ruleRecord.ruleName(),
                    0,
                    false,
                    "Rule execution error: " + e.getMessage(),
                    ruleRecord.weight().doubleValue()
                ));
            }
        }
        
        // Step 3: Calculate final rule score (0-100)
        double finalRuleScore = totalWeight > 0 ? (weightedScoreSum / totalWeight) : 0;
        
        // Step 4: Build explanation
        String explanation = buildExplanation(triggeredRuleNames, ruleScores);
        
        logger.debug("Payment {} fraud rule evaluation complete. Final score: {}, Triggered rules: {}", 
            payment.id(), finalRuleScore, triggeredRuleNames);
        
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
     * Get all registered fraud rule names from the registry.
     * Note: Actual rule instances are now loaded from database on each evaluation.
     * This method returns only the names of registered implementations.
     */
    public List<String> getRegisteredRuleNames() {
        return new ArrayList<>(ruleRegistry.getRegisteredRuleNames());
    }
    
    /**
     * Get registry size (number of registered rule implementations).
     */
    public int getRegistrySize() {
        return ruleRegistry.getRegistrySize();
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

