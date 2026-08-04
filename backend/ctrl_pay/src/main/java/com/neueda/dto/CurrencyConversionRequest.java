package com.neueda.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO for currency conversion.
 */
public record CurrencyConversionRequest(
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be positive")
    BigDecimal amount,
    
    @NotBlank(message = "Source currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-character ISO 4217 code")
    String sourceCurrency,
    
    @NotBlank(message = "Destination currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-character ISO 4217 code")
    String destinationCurrency
) {
}

