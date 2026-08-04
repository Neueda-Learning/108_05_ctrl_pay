package com.neueda.exception;

/**
 * Exception thrown when account validation fails.
 */
public class AccountValidationException extends RuntimeException {

    private final String errorCode;

    public AccountValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

