package com.neueda.domain;

/**
 * Validation rule severity level.
 * Determines whether a failed validation rule blocks payment processing or only generates a warning.
 */
public enum Severity {
    /**
     * Rule blocks payment processing.
     * If a HARD rule fails, the payment transitions to FAILED status immediately.
     */
    HARD("Hard - Blocks Payment"),
    
    /**
     * Rule generates a warning but does not block payment processing.
     * A SOFT rule failure does not prevent the payment from proceeding (future enhancement).
     */
    SOFT("Soft - Warning Only");

    private final String description;

    Severity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

