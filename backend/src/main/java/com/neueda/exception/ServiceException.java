package com.neueda.exception;

/**
 * Generic exception for service-level errors not related to payment processing.
 * Used for errors in customer profile, account operations, and other non-payment services.
 */
public class ServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public ServiceException(String message) {
        super(message);
        this.errorCode = "SERVICE_ERROR";
    }
    
    public ServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SERVICE_ERROR";
    }
    
    public ServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

