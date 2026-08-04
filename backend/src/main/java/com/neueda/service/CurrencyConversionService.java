package com.neueda.service;

import java.math.BigDecimal;

import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;

/**
 * Service for converting amounts between different currencies.
 * 
 * This is an abstraction that allows easy replacement with real exchange-rate APIs.
 * Current implementation uses mock rates stored in a configuration map.
 * 
 * Future implementations can:
 * - Call external APIs (e.g., Open Exchange Rates, Fixer.io)
 * - Connect to real-time market data
 * - Cache conversion rates
 * 
 * Example of replacement:
 * Replace MockCurrencyConversionService with ExternalCurrencyConversionService
 * No changes needed in controllers, services, or frontend code.
 */
public interface CurrencyConversionService {
    
    /**
     * Convert an amount from one currency to another.
     * 
     * @param request contains amount, source currency, and destination currency
     * @return conversion response with exchange rate and converted amount
     */
    CurrencyConversionResponse convert(CurrencyConversionRequest request);
}

