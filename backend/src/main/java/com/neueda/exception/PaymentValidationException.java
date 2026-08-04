package com.neueda.exception;

/**
 * Exception thrown when payment validation fails.
 */
public class PaymentValidationException extends RuntimeException {
    
    private final String errorCode;
    
    public PaymentValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

