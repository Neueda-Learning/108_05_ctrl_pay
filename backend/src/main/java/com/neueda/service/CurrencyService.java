package com.neueda.service;

import java.util.List;

import com.neueda.dto.CurrencyResponse;

/**
 * Service for managing currency data.
 * 
 * Responsibilities:
 * 1. Load currencies from external API during application startup
 * 2. Cache currencies in memory to avoid repeated API calls
 * 3. Provide list of available currencies to frontend
 * 4. Filter out invalid/empty currencies
 * 
 * Implementation:
 * - Uses external API: https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.json
 * - Caches results in-memory
 * - Filters out empty or null currency names
 */
public interface CurrencyService {
    
    /**
     * Get list of all available currencies.
     * Returns cached data if available, loads from API on first call.
     * 
     * @return list of currency codes and names
     */
    List<CurrencyResponse> getAllCurrencies();
    
    /**
     * Check if a currency is valid (in the available currencies list).
     * 
     * @param currencyCode ISO 4217 currency code (e.g., USD, EUR)
     * @return true if currency is supported
     */
    boolean isValidCurrency(String currencyCode);
    
    /**
     * Force refresh of currencies from external API.
     * Useful for testing or manual updates.
     */
    void refreshCurrencies();
}

