package com.neueda.domain;

/**
 * Fraud decision enumeration.
 * Represents the final decision from fraud detection system.
 */
public enum FraudDecision {
    /**
     * Payment approved - low fraudulent risk
     */
    APPROVED("Approved"),
    
    /**
     * Payment requires manual review by administrator
     */
    SUSPICIOUS("Suspicious"),
    
    /**
     * Payment rejected - high fraudulent risk detected
     */
    REJECTED("Rejected");

    private final String displayName;

    FraudDecision(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

