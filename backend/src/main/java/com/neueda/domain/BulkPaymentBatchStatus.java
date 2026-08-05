package com.neueda.domain;

/**
 * Bulk payment batch lifecycle status enumeration.
 * 
 * States:
 * CREATED -> initial state after upload
 * VALIDATING -> batch is in validation phase
 * VALIDATED -> all items passed validation
 * PROCESSING -> batch is being settled
 * COMPLETED -> all transactions settled successfully
 * PARTIALLY_COMPLETED -> some transactions succeeded, some failed
 * FAILED -> batch processing failed at validation/processing phase
 * ROLLED_BACK -> batch transactions were rolled back
 */
public enum BulkPaymentBatchStatus {
    CREATED("Batch created, awaiting validation"),
    VALIDATING("Batch is being validated"),
    VALIDATED("All items validated successfully"),
    PROCESSING("Batch is being processed for payment settlement"),
    COMPLETED("All transactions completed successfully"),
    PARTIALLY_COMPLETED("Some transactions succeeded, some failed"),
    FAILED("Batch failed validation or processing"),
    ROLLED_BACK("Batch transactions were rolled back");
    
    private final String description;
    
    BulkPaymentBatchStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

