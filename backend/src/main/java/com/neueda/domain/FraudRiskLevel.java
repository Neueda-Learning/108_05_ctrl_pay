package com.neueda.domain;

import java.math.BigDecimal;

/**
 * Fraud risk level enumeration.
 * Classifies the overall risk profile of a payment.
 */
public enum FraudRiskLevel {
    /**
     * Low risk - fraud score 0-30%
     */
    LOW("Low"),
    
    /**
     * Medium risk - fraud score 31-60%
     */
    MEDIUM("Medium"),
    
    /**
     * High risk - fraud score 61-90%
     */
    HIGH("High"),
    
    /**
     * Critical risk - fraud score 91-100%
     */
    CRITICAL("Critical");

    private final String displayName;

    FraudRiskLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public static FraudRiskLevel fromScore(BigDecimal score) {
        if (score == null) {
            return LOW;
        }
        double value = score.doubleValue();
        if (value < 30) {
            return LOW;
        } else if (value < 60) {
            return MEDIUM;
        } else if (value < 90) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
}


