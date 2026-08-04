package com.neueda.dto;

/**
 * Response DTO for currency information.
 * Used when returning list of available currencies.
 */
public record CurrencyResponse(
    /**
     * ISO 4217 currency code (e.g., USD, EUR, GBP).
     * Stored as uppercase 3-character code.
     */
    String code,
    
    /**
     * Display name of the currency (e.g., "US Dollar", "Euro").
     */
    String name
) {
}

