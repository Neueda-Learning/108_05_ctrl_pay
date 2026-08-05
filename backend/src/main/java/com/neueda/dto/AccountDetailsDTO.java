package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.neueda.domain.AccountStatus;

/**
 * DTO for complete account details with transactions and statistics.
 */
public record AccountDetailsDTO(
    // Basic information
    Long accountId,
    String accountNumber,
    String accountName,
    String accountType,
    String currency,
    LocalDate accountOpeningDate,
    AccountStatus status,
    String ifscCode,
    String bankName,
    String accountLocation,
    
    // Balance information
    BigDecimal currentBalance,
    BigDecimal availableBalance,
    BigDecimal blockedAmount,
    
    // Account activity
    List<TransactionSummaryDTO> recentTransactions,
    LocalDateTime lastPaymentDate,
    Long transactionCount
) {}

