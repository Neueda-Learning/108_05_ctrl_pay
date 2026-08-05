package com.neueda.exception;

/**
 * Base exception for bulk payment operations.
 */
public class BulkPaymentException extends RuntimeException {
    
    private final String errorCode;
    
    public BulkPaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BulkPaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

