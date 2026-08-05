package com.neueda.exception;

/**
 * Exception for bulk payment CSV validation errors.
 */
public class BulkPaymentCSVValidationException extends BulkPaymentException {
    
    public BulkPaymentCSVValidationException(String message) {
        super(message, "CSV_VALIDATION_FAILED");
    }
    
    public BulkPaymentCSVValidationException(String message, Throwable cause) {
        super(message, "CSV_VALIDATION_FAILED", cause);
    }
}

