package com.neueda.exception;

/**
 * Raised when a customer attempts to access a receipt for a payment they do not own.
 */
public class ReceiptAccessDeniedException extends RuntimeException {

    public ReceiptAccessDeniedException(String message) {
        super(message);
    }
}
