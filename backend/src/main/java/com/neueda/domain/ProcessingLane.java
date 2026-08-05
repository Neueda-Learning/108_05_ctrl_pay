package com.neueda.domain;

/**
 * Payment processing lane enumeration.
 * Determines how a payment is routed through the fraud detection and approval system.
 */
public enum ProcessingLane {
    /**
     * Fast track - automatically approved low-risk payments
     * SLA: 5 minutes or less
     */
    FAST_TRACK("Fast Track - Auto-Approved"),
    
    /**
     * Manual review - medium-risk payments requiring human judgment
     * SLA: 60 minutes
     */
    MANUAL_REVIEW("Manual Review Required"),
    
    /**
     * Escalation - high-risk payments requiring urgent review
     * SLA: 120 minutes or immediate escalation
     */
    ESCALATION("Escalation Required"),
    
    /**
     * Rejection - automatically rejected critical-risk payments
     * SLA: Immediate
     */
    REJECTION("Auto-Rejected");
    
    private final String displayName;
    
    ProcessingLane(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get processing lane from hybrid fraud score
     */
    public static ProcessingLane fromFraudScore(double scorePercentage) {
        if (scorePercentage < 30) {
            return FAST_TRACK;
        } else if (scorePercentage < 75) {
            return MANUAL_REVIEW;
        } else if (scorePercentage < 95) {
            return ESCALATION;
        } else {
            return REJECTION;
        }
    }
    
    /**
     * Get SLA in minutes for this lane
     */
    public long getSlaMinutes() {
        return switch(this) {
            case FAST_TRACK -> 5;
            case MANUAL_REVIEW -> 60;
            case ESCALATION -> 120;
            case REJECTION -> 0;  // Immediate
        };
    }
}

