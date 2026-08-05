package com.neueda.dto;

import java.math.BigDecimal;

/**
 * DTO for individual bulk payment item (CSV row or manual entry).
 */
public record BulkPaymentItemDTO(
    /**
     * Destination account number (12 digits).
     */
    String destinationAccount,
    
    /**
     * Payment amount.
     */
    BigDecimal amount,
    
    /**
     * ISO 4217 currency code (e.g., USD, EUR).
     */
    String currency,
    
    /**
     * Optional transaction description/memo.
     */
    String description
) {}

