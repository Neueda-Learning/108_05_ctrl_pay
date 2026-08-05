package com.neueda.exception;

/**
 * Exception for bulk batch not found.
 */
public class BulkPaymentBatchNotFoundException extends BulkPaymentException {
    
    public BulkPaymentBatchNotFoundException(String batchReference) {
        super("Bulk payment batch not found: " + batchReference, "BATCH_NOT_FOUND");
    }
    
    public BulkPaymentBatchNotFoundException(Long batchId) {
        super("Bulk payment batch not found: " + batchId, "BATCH_NOT_FOUND");
    }
}

