package com.neueda.validation.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.PaymentRecord;

/**
 * Interface for a validation rule implementation.
 * Each rule type (AMOUNT_RANGE, CURRENCY_WHITELIST, etc.) has its own implementation.
 */
public interface ValidationRule {
    
    /**
     * Execute this validation rule against a payment.
     * 
     * @param payment payment to validate
     * @param ruleDefinition JSON rule configuration
     * @return ValidationRuleResult containing pass/fail status and any error details
     */
    ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition);
    
    /**
     * Result of executing a single validation rule.
     */
    record ValidationRuleResult(
        boolean passed,
        String errorCode,
        String errorMessage,
        long executionTimeMs
    ) {
        /**
         * Create a passing result.
         */
        public static ValidationRuleResult success(long executionTimeMs) {
            return new ValidationRuleResult(true, null, null, executionTimeMs);
        }
        
        /**
         * Create a failing result.
         */
        public static ValidationRuleResult failure(String errorCode, String errorMessage, long executionTimeMs) {
            return new ValidationRuleResult(false, errorCode, errorMessage, executionTimeMs);
        }
    }
}

