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
            BigDecimal min = new BigDecimal(ruleDefinition.get("min").asText());
            BigDecimal max = new BigDecimal(ruleDefinition.get("max").asText());
            String errorMessage = ruleDefinition.get("message").asText("Amount out of range");

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


