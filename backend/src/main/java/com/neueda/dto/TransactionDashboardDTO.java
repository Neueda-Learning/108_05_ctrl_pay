package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Transaction Dashboard statistics.
 */
public record TransactionDashboardDTO(
    Long totalTransactionCount,
    BigDecimal transactionVolume,
    BigDecimal averageTransactionValue,
    List<DailyTransactionTrendDTO> dailyTransactionTrend,
    List<MonthlyTransactionTrendDTO> monthlyTransactionTrend,
    Map<String, Long> transactionsByCurrency,
    Map<String, Long> transactionsByStatus,
    Map<String, Long> peakTransactionHours,
    LocalDateTime timestamp
) {
    
    public record DailyTransactionTrendDTO(
        String date,
        Long count,
        BigDecimal volume
    ) {}
    
    public record MonthlyTransactionTrendDTO(
        String month,
        Long count,
        BigDecimal volume
    ) {}
}

