package com.neueda.fraud.rules;

/**
 * Result of evaluating a single fraud rule.
 */
public record FraudRuleResult(
    /**
     * Name of the rule that was evaluated
     */
    String ruleName,
    
    /**
     * Score contribution from this rule (0-100)
     */
    int score,
    
    /**
     * Whether this rule was triggered (score > 0)
     */
    boolean triggered,
    
    /**
     * Explanation of why this score was assigned
     */
    String explanation
) {
    /**
     * Create a result where rule was not triggered
     */
    public static FraudRuleResult notTriggered(String ruleName, String reason) {
        return new FraudRuleResult(ruleName, 0, false, reason);
    }
    
    /**
     * Create a result where rule was triggered
     */
    public static FraudRuleResult triggered(String ruleName, int score, String reason) {
        return new FraudRuleResult(ruleName, Math.min(100, Math.max(0, score)), true, reason);
    }
}

