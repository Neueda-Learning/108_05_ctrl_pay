package com.neueda.validation.rules.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.validation.rules.ValidationRule;

/**
 * Validates that source and destination accounts are different.
 * Rule Type: ACCOUNT_DIFFERENCE
 * Rule Definition: { "type": "ACCOUNT_DIFFERENCE", "message": "..." }
 */
public class AccountDifferenceRule implements ValidationRule {
    
    @Override
    public ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition) {
        long startTime = System.currentTimeMillis();
        
        try {
            String errorMessage = ruleDefinition.get("message").asText("Source and destination accounts must be different");
            
            if (payment.sourceAccount().equals(payment.destinationAccount())) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_ACCOUNT", errorMessage, executionTime);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.success(executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("VALIDATION_ERROR", "Error executing ACCOUNT_DIFFERENCE rule: " + e.getMessage(), executionTime);
        }
    }
}

