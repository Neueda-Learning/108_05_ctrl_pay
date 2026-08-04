package com.neueda.domain;

/**
 * Standardized error codes for payment processing failures.
 * These codes are used in API error responses and stored in the database for auditing.
 */
public enum ErrorCode {
    /**
     * Payment failed validation checks (validation rule failed).
     * HTTP Status: 400 Bad Request
     */
    VALIDATION_FAILED("VALIDATION_FAILED", "Payment failed validation checks"),
    
    /**
     * Source account has insufficient funds.
     * HTTP Status: 400 Bad Request
     */
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "Source account has insufficient funds"),
    
    /**
     * Account number is invalid or doesn't exist.
     * HTTP Status: 400 Bad Request
     */
    INVALID_ACCOUNT("INVALID_ACCOUNT", "Account number is invalid or doesn't exist"),
    
    /**
     * Currency code is not supported.
     * HTTP Status: 400 Bad Request
     */
    INVALID_CURRENCY("INVALID_CURRENCY", "Currency code is not supported"),
    
    /**
     * Payment amount is zero, negative, or exceeds maximum.
     * HTTP Status: 400 Bad Request
     */
    INVALID_AMOUNT("INVALID_AMOUNT", "Payment amount is invalid"),
    
    /**
     * Payment with same idempotency key exists with different details.
     * HTTP Status: 409 Conflict
     */
    DUPLICATE_PAYMENT("DUPLICATE_PAYMENT", "Payment with same idempotency key exists"),
    
    /**
     * Cannot transition from current status to requested status.
     * HTTP Status: 400 Bad Request
     */
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Cannot transition from current status to requested status"),
    
    /**
     * Payment ID does not exist.
     * HTTP Status: 404 Not Found
     */
    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "Payment ID does not exist"),
    
    /**
     * Internal error during payment processing.
     * HTTP Status: 500 Internal Server Error
     */
    PROCESSING_ERROR("PROCESSING_ERROR", "Internal error during payment processing"),
    
    /**
     * Communication failure with payment network.
     * HTTP Status: 503 Service Unavailable
     */
    NETWORK_ERROR("NETWORK_ERROR", "Communication failure with payment network");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

