package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for completed bulk payment batch.
 */
public record BulkPaymentResponseDTO(
    /**
     * Backend assigned batch ID.
     */
    Long batchId,
    
    /**
     * User-facing batch reference.
     */
    String batchReference,
    
    /**
     * Source account.
     */
    String sourceAccount,
    
    /**
     * Total transactions in batch.
     */
    Integer totalTransactions,
    
    /**
     * Count of successful transactions.
     */
    Integer successfulCount,
    
    /**
     * Count of failed transactions.
     */
    Integer failedCount,
    
    /**
     * Batch status.
     */
    String status,
    
    /**
     * Total amount of all transactions.
     */
    BigDecimal totalAmount,
    
    /**
     * Batch creation timestamp.
     */
    LocalDateTime createdAt,
    
    /**
     * Batch completion timestamp.
     */
    LocalDateTime completedAt,
    
    /**
     * Results for each transaction.
     */
    List<BulkTransactionResultDTO> transactionResults
) {}

