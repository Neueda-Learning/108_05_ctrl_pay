package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Platform Overview Dashboard statistics.
 */
public record DashboardOverviewDTO(
    Long totalTransactions,
    BigDecimal totalTransactionVolume,
    Long successfulTransactions,
    Long failedTransactions,
    Long pendingTransactions,
    Double transactionSuccessRate,
    Long fraudDetectedCount,
    Long activeCustomers,
    Long activeAccounts,
    String systemHealth,
    LocalDateTime timestamp
) {}

