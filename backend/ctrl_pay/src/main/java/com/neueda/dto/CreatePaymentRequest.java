package com.neueda.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a new payment.
 * Received from API clients when submitting a payment for processing.
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
     * Payment amount. Must be greater than zero and not exceed 1,000,000.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount,
    
    /**
     * ISO 4217 currency code (e.g., USD, EUR, GBP). Must be exactly 3 uppercase characters.
     */
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-character ISO 4217 code")
    String currency,
    
    /**
     * Optional idempotency key for duplicate prevention.
     * If provided, the same key with same payment details will return the same payment (200 OK).
     * If provided with different details, will return 409 Conflict.
     */
    String idempotencyKey
) {
}

