package com.neueda.dto;

import java.math.BigDecimal;

/**
 * DTO for individual transaction result within a bulk batch.
 */
public record BulkTransactionResultDTO(
    /**
     * Line number in CSV (1-indexed).
     */
    Integer lineNumber,
    
    /**
     * Payment ID assigned to this transaction (if created).
     */
    Long paymentId,
    
    /**
     * Destination account.
     */
    String destinationAccount,
    
    /**
     * Transaction amount.
     */
    BigDecimal amount,
    
    /**
     * Currency code.
     */
    String currency,
    
    /**
     * Transaction status: SUCCESS, FAILED, ROLLED_BACK.
     */
    String status,
    
    /**
     * Failure reason if status is FAILED.
     */
    String failureReason,
    
    /**
     * Error code if validation/processing failed.
     */
    String errorCode,
    
    /**
     * Fraud score assigned to this transaction (0-100).
     */
    BigDecimal fraudScore,
    
    /**
     * Fraud decision: APPROVED, SUSPICIOUS, REJECTED.
     */
    String fraudDecision,
    
    /**
     * JSON array of validation error messages.
     */
    String validationErrors,
    
    /**
     * Rollback status if transaction was rolled back.
     */
    String rollbackStatus
) {}

