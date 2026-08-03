package com.neueda.validation.rules.impl;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.validation.rules.ValidationRule;

/**
 * Validates that account numbers match the expected format.
 * Rule Type: ACCOUNT_FORMAT
 * Rule Definition: { "type": "ACCOUNT_FORMAT", "pattern": "^[0-9]{12}$", "message": "..." }
 */
public class AccountFormatRule implements ValidationRule {
    
    @Override
    public ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition) {
        long startTime = System.currentTimeMillis();
        
        try {
            JsonNode patternNode = ruleDefinition.get("pattern");
            String pattern = (patternNode != null) ? patternNode.asText("^[0-9]{12}$") : "^[0-9]{12}$";
            
            JsonNode messageNode = ruleDefinition.get("message");
            String errorMessage = (messageNode != null) ? messageNode.asText("Invalid account format") 
                                                        : "Invalid account format";
            
            // Check for null account values
            if (payment.sourceAccount() == null || payment.destinationAccount() == null) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_ACCOUNT", "Source or destination account is null", executionTime);
            }
            
            Pattern regex = Pattern.compile(pattern);
            
            // Check both source and destination accounts
            if (!regex.matcher(payment.sourceAccount()).matches() || !regex.matcher(payment.destinationAccount()).matches()) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_ACCOUNT", errorMessage, executionTime);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.success(executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("VALIDATION_ERROR", "Error executing ACCOUNT_FORMAT rule: " + e.getMessage(), executionTime);
        }
    }
}

