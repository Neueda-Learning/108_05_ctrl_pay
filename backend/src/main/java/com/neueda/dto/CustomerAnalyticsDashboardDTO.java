package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Customer Analytics Dashboard.
 */
public record CustomerAnalyticsDashboardDTO(
    Long totalCustomers,
    Long activeCustomers,
    Long newRegistrationsThisMonth,
    Long totalAccounts,
    BigDecimal averageAccountBalance,
    Long customersWithMultipleAccounts,
    List<TopCustomerDTO> topCustomersByTransactionVolume,
    LocalDateTime timestamp
) {

    public record TopCustomerDTO(
        Long customerId,
        String customerName,
        Long transactionCount,
        BigDecimal totalVolume
    ) {}
}

