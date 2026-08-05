package com.neueda.domain;

/**
 * Individual bulk payment item lifecycle status.
 * 
 * States:
 * PENDING -> item created from CSV upload
 * VALIDATING -> item validation in progress
 * VALIDATED -> item passed validation
 * PROCESSING -> payment settlement in progress
 * SUCCESS -> payment settled successfully
 * FAILED -> validation or settlement failed
 * ROLLED_BACK -> payment was rolled back
 */
public enum BulkPaymentItemStatus {
    PENDING("Item pending validation"),
    VALIDATING("Item is being validated"),
    VALIDATED("Item validation passed"),
    PROCESSING("Item payment is being processed"),
    SUCCESS("Item payment settled successfully"),
    FAILED("Item validation or payment failed"),
    ROLLED_BACK("Item payment was rolled back");
    
    private final String description;
    
    BulkPaymentItemStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

