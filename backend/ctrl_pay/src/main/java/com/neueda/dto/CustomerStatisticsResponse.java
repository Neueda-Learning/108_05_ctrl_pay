package com.neueda.dto;

/**
 * Customer Statistics Response DTO.
 * 
 * Provides summary statistics for a customer's payments:
 * - Total payments (all statuses)
 * - Completed payments (COMPLETED status)
 * - Failed payments (FAILED status)
 * - Pending payments (CREATED, VALIDATED, SENT statuses)
 * - Account count
 */
public record CustomerStatisticsResponse(
    Long customerId,
    Long totalPayments,
    Long completedPayments,
    Long failedPayments,
    Long pendingPayments,
    Long accountCount
) {
}

