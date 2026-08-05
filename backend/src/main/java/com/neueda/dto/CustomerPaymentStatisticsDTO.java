package com.neueda.dto;

import java.math.BigDecimal;

/**
 * DTO for customer payment statistics.
 */
public record CustomerPaymentStatisticsDTO(
    Long totalPayments,
    Long successfulPayments,
    Long failedPayments,
    Long rejectedPayments,
    BigDecimal totalAmount,
    BigDecimal averageTransactionAmount
) {}

