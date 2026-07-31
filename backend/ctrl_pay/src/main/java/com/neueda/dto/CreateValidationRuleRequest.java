package com.neueda.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new validation rule.
 */
public record CreateValidationRuleRequest(
    /**
     * Unique rule name (e.g., AMOUNT_RANGE, CURRENCY_WHITELIST).
     */
    @NotBlank(message = "Rule name is required")
    String name,
    
    /**
     * Human-readable description of what the rule validates.
     */
    String description,
    
    /**
     * Rule type (AMOUNT_RANGE, CURRENCY_WHITELIST, ACCOUNT_FORMAT, etc.).
     */
    @NotBlank(message = "Rule type is required")
    String ruleType,
    
    /**
     * JSON rule definition with rule-specific parameters.
     */
    @NotNull(message = "Rule definition is required")
    JsonNode ruleDefinition,
    
    /**
     * Rule severity: HARD (blocks payment) or SOFT (warning only).
     */
    @NotBlank(message = "Severity is required")
    String severity,
    
    /**
     * Execution order (lower numbers execute first).
     */
    int orderOfExecution
) {
}

