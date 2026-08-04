package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.neueda.domain.AccountStatus;

/**
 * Response DTO for account details.
 * PIN hash is intentionally excluded from API responses.
 */
public record AccountResponse(
    Long accountId,
    Long customerId,
    String accountName,
    BigDecimal accountBalance,
    AccountStatus accountStatus,
    String currency,
    LocalDate accountOpeningDate,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastUpdated,
    String ifscCode,
    String accountLocation,
    String bankName
) {
}

