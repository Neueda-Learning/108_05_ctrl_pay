package com.neueda.validation.rules.impl;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.validation.rules.ValidationRule;

/**
 * Validates that payment amount is within acceptable range.
 * Rule Type: AMOUNT_RANGE
 * Rule Definition: { "type": "AMOUNT_RANGE", "min": 0.01, "max": 1000000.00, "message": "..." }
 */
public class AmountRangeRule implements ValidationRule {

    @Override
    public ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition) {
        long startTime = System.currentTimeMillis();

        try {
            // Extract min/max with null safety
            JsonNode minNode = ruleDefinition.get("min");
            JsonNode maxNode = ruleDefinition.get("max");
            
            if (minNode == null || maxNode == null) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("RULE_CONFIG_ERROR", 
                    "Rule configuration missing 'min' or 'max' field", executionTime);
            }
            
            BigDecimal min = new BigDecimal(minNode.asText());
            BigDecimal max = new BigDecimal(maxNode.asText());
            
            JsonNode messageNode = ruleDefinition.get("message");
            String errorMessage = (messageNode != null) ? messageNode.asText("Amount out of range") 
                                                        : "Amount out of range";

            if (payment.amount() == null) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_AMOUNT", "Payment amount is null", executionTime);
            }

            if (payment.amount().compareTo(min) < 0 || payment.amount().compareTo(max) > 0) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_AMOUNT", errorMessage, executionTime);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.success(executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("VALIDATION_ERROR", "Error executing AMOUNT_RANGE rule: " + e.getMessage(), executionTime);
        }
    }
}


