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
            // Extract allowed currencies from JSON array - with null safety
            Set<String> allowedCurrencies = new HashSet<>();
            JsonNode currenciesNode = ruleDefinition.get("allowed_currencies");
            
            // Check if currenciesNode exists and is an array
            if (currenciesNode != null && currenciesNode.isArray()) {
                for (JsonNode currency : currenciesNode) {
                    allowedCurrencies.add(currency.asText());
                }
            } else if (currenciesNode == null) {
                // Missing allowed_currencies configuration
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("RULE_CONFIG_ERROR", 
                    "Rule configuration missing 'allowed_currencies' field", executionTime);
            }
            
            JsonNode messageNode = ruleDefinition.get("message");
            String errorMessage = (messageNode != null) ? messageNode.asText("Currency is not supported") 
                                                        : "Currency is not supported";
            
            // Check if payment currency is in allowlist
            if (payment.currency() == null) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_CURRENCY", "Payment currency is null", executionTime);
            }
            
            if (!allowedCurrencies.contains(payment.currency())) {
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INVALID_CURRENCY", errorMessage, executionTime);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.success(executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("VALIDATION_ERROR", 
                "Error executing CURRENCY_WHITELIST rule: " + e.getMessage(), executionTime);
        }
    }
}

