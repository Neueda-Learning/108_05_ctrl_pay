package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.domain.BulkPaymentBatchStatus;

/**
 * DTO for bulk payment batch summary in customer profile.
 */
public record BulkPaymentSummaryDTO(
    Long batchId,
    String batchReference,
    String sourceAccount,
    Integer totalTransactions,
    Integer successfulTransactions,
    Integer failedTransactions,
    BigDecimal totalAmount,
    BulkPaymentBatchStatus status,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}

