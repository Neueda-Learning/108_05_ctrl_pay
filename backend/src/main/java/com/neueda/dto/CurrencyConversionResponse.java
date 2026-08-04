package com.neueda.dto;

import java.math.BigDecimal;

/**
 * Response DTO for currency conversion.
 */
public record CurrencyConversionResponse(
    String sourceCurrency,
    String destinationCurrency,
    BigDecimal sourceAmount,
    BigDecimal exchangeRate,
    BigDecimal convertedAmount
) {
}

