package com.neueda.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for batch status polling (real-time progress tracking).
 */
public record BulkPaymentProgressDTO(
    /**
     * Batch ID.
     */
    Long batchId,
    
    /**
     * Batch reference.
     */
    String batchReference,
    
    /**
     * Current batch status.
     */
    String status,
    
    /**
     * Total transactions.
     */
    Integer totalTransactions,
    
    /**
     * Number of completed validations.
     */
    Integer validatedCount,
    
    /**
     * Number of successful settlements.
     */
    Integer successfulCount,
    
    /**
     * Number of failed settlements.
     */
    Integer failedCount,
    
    /**
     * Percentage progress (0-100).
     */
    Integer progressPercentage,
    
    /**
     * Timestamp of last update.
     */
    LocalDateTime lastUpdatedAt,
    
    /**
     * Last error message if any.
     */
    String lastErrorMessage
) {}

