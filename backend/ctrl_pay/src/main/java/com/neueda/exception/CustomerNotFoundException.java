package com.neueda.exception;

/**
 * Exception thrown when a customer profile is not found.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}

