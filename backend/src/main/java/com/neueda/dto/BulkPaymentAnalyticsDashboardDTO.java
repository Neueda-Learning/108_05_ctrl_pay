package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Bulk Payment Analytics Dashboard.
 */
public record BulkPaymentAnalyticsDashboardDTO(
    Long totalBatchesCreated,
    Long totalPaymentsInBatches,
    Long successfulPayments,
    Long failedPayments,
    Long rollbackCount,
    BigDecimal totalAmountProcessed,
    BigDecimal totalAmountRolledBack,
    Map<String, Long> commonFailureReasons,
    Double averageBatchExecutionTimeSeconds,
    List<RecentBulkPaymentBatchDTO> recentBatches,
    LocalDateTime timestamp
) {

    public record RecentBulkPaymentBatchDTO(
        Long batchId,
        String batchReference,
        Long itemCount,
        String status,
        LocalDateTime createdAt
    ) {}
}

