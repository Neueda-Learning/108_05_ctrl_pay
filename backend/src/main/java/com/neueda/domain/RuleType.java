package com.neueda.domain;

/**
 * Enumeration of validation rule types.
 * Each rule type corresponds to a specific validation logic that can be implemented as a ValidationRule.
 */
public enum RuleType {
    /**
     * Validates that payment amount is within acceptable range.
     * Rule Parameters: min, max
     */
    AMOUNT_RANGE("Amount Range Validation"),
    
    /**
     * Validates that currency is in the supported list.
     * Rule Parameters: allowed_currencies (array)
     */
    CURRENCY_WHITELIST("Currency Whitelist Validation"),
    
    /**
     * Validates that account numbers match expected format.
     * Rule Parameters: pattern (regex)
     */
    ACCOUNT_FORMAT("Account Format Validation"),
    
    /**
     * Validates that source and destination accounts are different.
     * Rule Parameters: none
     */
    ACCOUNT_DIFFERENCE("Account Difference Validation"),
    
    /**
     * Validates that source account has sufficient funds for the payment.
     * Performs real balance validation by checking actual account data.
     * Rule Parameters: none (uses account data from database)
     */
    SUFFICIENT_FUNDS("Sufficient Funds Validation");

    private final String description;

    RuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

