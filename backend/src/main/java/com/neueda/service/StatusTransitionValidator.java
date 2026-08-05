package com.neueda.service;

import com.neueda.domain.PaymentStatus;

/**
 * Validator for payment status transitions.
 * Enforces the payment lifecycle state machine:
 * CREATED → VALIDATED → SENT → COMPLETED/FAILED
 * 
 * Invalid transitions are rejected with clear error messages.
 */
public class StatusTransitionValidator {
    
    /**
     * Validate that a status transition is allowed.
     * 
     * @param fromStatus current status
     * @param toStatus desired status
     * @throws IllegalStateException if transition is not allowed
     */
    public static void validateTransition(PaymentStatus fromStatus, PaymentStatus toStatus) {
        if (!isTransitionAllowed(fromStatus, toStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", fromStatus, toStatus)
            );
        }
    }
    
    /**
     * Validator for payment status transitions.
     * Enforces the payment lifecycle state machine:
     * CREATED → VALIDATED/SUSPICIOUS → SENT → COMPLETED/FAILED
     * SUSPICIOUS → VALIDATED/FAILED (admin review)
     * 
     * Invalid transitions are rejected with clear error messages.
     */
    public static boolean isTransitionAllowed(PaymentStatus fromStatus, PaymentStatus toStatus) {
        return switch (fromStatus) {
            case CREATED -> toStatus == PaymentStatus.VALIDATED || 
                           toStatus == PaymentStatus.SUSPICIOUS || 
                           toStatus == PaymentStatus.FAILED;
            case VALIDATED -> toStatus == PaymentStatus.SENT || 
                             toStatus == PaymentStatus.SUSPICIOUS || 
                             toStatus == PaymentStatus.FAILED;
            case SUSPICIOUS -> toStatus == PaymentStatus.VALIDATED || 
                              toStatus == PaymentStatus.FAILED; // Admin review decision
            case SENT -> toStatus == PaymentStatus.COMPLETED || toStatus == PaymentStatus.FAILED;
            case COMPLETED, FAILED -> false; // Terminal states - no transitions allowed
        };
    }
}

