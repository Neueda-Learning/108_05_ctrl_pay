package com.neueda.exception;

/**
 * Exception thrown when customer validation fails.
 */
public class CustomerValidationException extends RuntimeException {

    private final String errorCode;

    public CustomerValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

