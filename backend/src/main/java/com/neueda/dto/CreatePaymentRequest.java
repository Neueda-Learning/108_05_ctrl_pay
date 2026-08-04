package com.neueda.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO for creating a new payment.
 * Received from API clients when submitting a payment for processing.
 * 
 * Includes exchange rate information calculated by the frontend during currency conversion.
 * This ensures the exchange rate used at payment initiation is preserved for settlement.
 */
public record CreatePaymentRequest(
    /**
     * Source account number (must be 12 digits).
     */
    @NotBlank(message = "Source account is required")
    String sourceAccount,
    
    /**
     * Destination account number (must be 12 digits and different from source).
     */
    @NotBlank(message = "Destination account is required")
    String destinationAccount,
    
    /**
     * Payment amount in source account currency. Must be greater than zero and not exceed 1,000,000.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,
    
    /**
     * ISO 4217 currency code (e.g., USD, EUR, GBP). Must be exactly 3 uppercase characters.
     * This is the currency of the source account.
     */
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-character ISO 4217 code")
    String currency,
    
    /**
     * Optional idempotency key for duplicate prevention.
     * If provided, the same key with same payment details will return the same payment (200 OK).
     * If provided with different details, will return 409 Conflict.
     */
    String idempotencyKey,
    
    /**
     * PIN for source account authentication (4-6 digits).
     * Required to verify account ownership before processing payment.
     */
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4 to 6 digits")
    String pin,
    
    /**
     * Currency of source account (optional, for cross-currency transfers).
     */
    String sourceCurrency,
    
    /**
     * Currency of destination account (optional, for cross-currency transfers).
     */
    String destinationCurrency,
    
    /**
     * Amount debited from source account in source currency (optional, calculated during conversion).
     */
    @PositiveOrZero(message = "Source amount must be positive or zero")
    BigDecimal sourceAmount,
    
    /**
     * Amount credited to destination account in destination currency (optional, calculated during conversion).
     */
    @PositiveOrZero(message = "Destination amount must be positive or zero")
    BigDecimal destinationAmount,
    
    /**
     * Exchange rate applied during conversion (optional, calculated during conversion).
     * 1 source unit = ? destination units
     */
    @PositiveOrZero(message = "Exchange rate must be positive or zero")
    BigDecimal exchangeRate
) {
}

