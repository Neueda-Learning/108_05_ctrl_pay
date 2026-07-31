package com.neueda.dto;

/**
 * Response DTO for validation rule execution result.
 * Embedded in PaymentResponse to show what validations were performed and their outcomes.
 */
public record ValidationResultResponse(
    /**
     * ID of the validation rule that was executed.
     */
    Long ruleId,
    
    /**
     * Name of the validation rule (e.g., AMOUNT_RANGE, CURRENCY_WHITELIST).
     */
    String ruleName,
    
    /**
     * Whether this validation passed (true) or failed (false).
     */
    boolean passed,
    
    /**
     * Error code if validation failed (null if passed).
     */
    String errorCode,
    
    /**
     * Human-readable error message if validation failed (null if passed).
     */
    String errorMessage,
    
    /**
     * Time taken to execute this rule in milliseconds.
     */
    int executionTimeMs
) {
}

