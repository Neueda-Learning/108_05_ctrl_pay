package com.neueda.validation.rules.impl;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.validation.rules.ValidationRule;

/**
 * Validates that payment currency is in the supported list.
 * Rule Type: CURRENCY_WHITELIST
 * Rule Definition: { "type": "CURRENCY_WHITELIST", "allowed_currencies": ["USD", "EUR", ...], "message": "..." }
 */
public class CurrencyWhitelistRule implements ValidationRule {
    
    @Override
    public ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract allowed currencies from JSON array
            Set<String> allowedCurrencies = new HashSet<>();
            JsonNode currenciesNode = ruleDefinition.get("allowed_currencies");
            if (currenciesNode.isArray()) {
                for (JsonNode currency : currenciesNode) {
                    allowedCurrencies.add(currency.asText());
                }
            }
            
            String errorMessage = ruleDefinition.get("message").asText("Currency is not supported");
            
            if (!allowedCurrencies.contains(payment.currency())) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_CURRENCY", errorMessage, executionTime);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.success(executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("VALIDATION_ERROR", "Error executing CURRENCY_WHITELIST rule: " + e.getMessage(), executionTime);
        }
    }
}

