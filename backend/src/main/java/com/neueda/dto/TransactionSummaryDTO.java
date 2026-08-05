package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.domain.PaymentStatus;

/**
 * DTO for transaction summary in profile and account details.
 */
public record TransactionSummaryDTO(
    Long transactionId,
    String sourceAccount,
    String destinationAccount,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    LocalDateTime transactionDate,
    String errorCode
) {}

