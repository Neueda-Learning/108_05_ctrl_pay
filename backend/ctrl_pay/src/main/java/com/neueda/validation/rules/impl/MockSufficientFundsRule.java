package com.neueda.validation.rules.impl;

import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.PaymentRecord;
import com.neueda.validation.rules.ValidationRule;

/**
 * Simulates checking if source account has sufficient funds.
 * This is a MOCK implementation for testing purposes.
 * Rule Type: MOCK_SUFFICIENT_FUNDS
 * Rule Definition: { "type": "MOCK_SUFFICIENT_FUNDS", "failure_rate": 0.1, "message": "..." }
 */
public class MockSufficientFundsRule implements ValidationRule {

    private static final Random random = new Random();

    @Override
    public ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition) {
        long startTime = System.currentTimeMillis();

        try {
            // Extract failure rate with null safety (0.0 to 1.0)
            JsonNode failureRateNode = ruleDefinition.get("failure_rate");
            double failureRate = (failureRateNode != null) ? failureRateNode.asDouble(0.1) : 0.1;
            
            JsonNode messageNode = ruleDefinition.get("message");
            String errorMessage = (messageNode != null) ? messageNode.asText("Insufficient funds in source account") 
                                                        : "Insufficient funds in source account";

            // Simulate: random failure based on failure_rate
            // In real implementation, would query actual account balance
            double randomValue = random.nextDouble();

            if (randomValue < failureRate) {
                // Fail (simulated insufficient funds)
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INSUFFICIENT_FUNDS", errorMessage, executionTime);
            }

            // Pass (simulated sufficient funds)
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.success(executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("VALIDATION_ERROR", "Error executing MOCK_SUFFICIENT_FUNDS rule: " + e.getMessage(), executionTime);
        }
    }
}


