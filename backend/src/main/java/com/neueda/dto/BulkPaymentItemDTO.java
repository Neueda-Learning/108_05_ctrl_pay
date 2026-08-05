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
     * Optional - defaults to source account currency if not provided.
     */
    String currency,
    
    /**
     * Optional transaction description/memo.
     */
    String description
) {
    /**
     * Constructor with only required fields.
     * Currency and description default to null and will be set from source account.
     */
    public static BulkPaymentItemDTO create(String destinationAccount, BigDecimal amount) {
        return new BulkPaymentItemDTO(destinationAccount, amount, null, null);
    }
    
    /**
     * Constructor with currency specified.
     */
    public static BulkPaymentItemDTO create(String destinationAccount, BigDecimal amount, String currency) {
        return new BulkPaymentItemDTO(destinationAccount, amount, currency, null);
    }
}

