package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced status DTO for bulk payment batch with individual payment details.
 * Used for real-time UI polling with complete transaction information.
 */
public record BulkPaymentStatusDTO(
    /**
     * Batch ID.
     */
    Long batchId,
    
    /**
     * Batch reference.
     */
    String batchReference,
    
    /**
     * Source account.
     */
    String sourceAccount,
    
    /**
     * Current batch status.
     */
    String batchStatus,
    
    /**
     * Total transactions in batch.
     */
    Integer totalTransactions,
    
    /**
     * Number of transactions being processed.
     */
    Integer processingCount,
    
    /**
     * Number of completed transactions.
     */
    Integer completedCount,
    
    /**
     * Number of failed transactions.
     */
    Integer failedCount,
    
    /**
     * Overall progress percentage (0-100).
     */
    Integer progressPercentage,
    
    /**
     * Total amount of all transactions.
     */
    BigDecimal totalAmount,
    
    /**
     * Batch creation timestamp.
     */
    LocalDateTime createdAt,
    
    /**
     * Last update timestamp.
     */
    LocalDateTime lastUpdatedAt,
    
    /**
     * Individual payment transactions within the batch.
     */
    List<BatchPaymentStatusDTO> payments
) {
    /**
     * Nested DTO for individual payment status within a batch.
     */
    public record BatchPaymentStatusDTO(
        /**
         * Payment ID.
         */
        Long paymentId,
        
        /**
         * Idempotency key for this bulk payment item.
         */
        String idempotencyKey,
        
        /**
         * Line number in batch (1-indexed).
         */
        Integer lineNumber,
        
        /**
         * Destination account.
         */
        String destinationAccount,
        
        /**
         * Payment amount.
         */
        BigDecimal amount,
        
        /**
         * Currency code.
         */
        String currency,
        
        /**
         * Current payment status.
         */
        String status,
        
        /**
         * Failure reason if payment failed.
         */
        String failureReason,
        
        /**
         * Error code if any.
         */
        String errorCode
    ) {}
}

