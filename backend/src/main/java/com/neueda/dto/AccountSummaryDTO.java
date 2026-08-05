package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.neueda.domain.AccountStatus;

/**
 * DTO for account summary information in profile.
 */
public record AccountSummaryDTO(
    Long accountId,
    String accountNumber,
    String accountName,
    String accountType,
    String currency,
    BigDecimal balance,
    BigDecimal availableBalance,
    AccountStatus status,
    LocalDate accountOpeningDate
) {}

