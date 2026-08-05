package com.neueda.domain;

/**
 * Payment lifecycle status enumeration.
 * 
 * Valid transitions:
 * CREATED → VALIDATED or FAILED
 * VALIDATED → SENT or FAILED
 * SENT → COMPLETED or FAILED
 * COMPLETED → (terminal state)
 * FAILED → (terminal state)
 */
public enum PaymentStatus {
    /**
     * Payment has been submitted but not yet validated.
     */
    CREATED("Created"),
    
    /**
     * Payment has passed all validation rules and is ready to be sent.
     */
    VALIDATED("Validated"),
    
    /**
     * Payment is under fraud review by administrator.
     * Triggered by fraud detection engine when hybrid fraud score exceeds review threshold.
     */
    SUSPICIOUS("Suspicious"),
    
    /**
     * Payment has been transmitted to the destination system.
     */
    SENT("Sent"),
    
    /**
     * Payment has been successfully processed and confirmed.
     */
    COMPLETED("Completed"),
    
    /**
     * Payment has failed at some point in the process.
     */
    FAILED("Failed");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

