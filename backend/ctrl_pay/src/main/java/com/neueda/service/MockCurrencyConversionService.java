package com.neueda.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.exception.PaymentValidationException;

/**
 * Mock implementation of CurrencyConversionService.
 * 
 * Uses hardcoded exchange rates for demonstration and testing.
 * 
 * IMPORTANT: This is a temporary mock implementation.
 * It is designed to be easily replaceable with a real service:
 * 
 * To replace:
 * 1. Create ExternalCurrencyConversionService extends CurrencyConversionService
 * 2. Inject external API calls
 * 3. Update @Service annotation or add @Primary to new implementation
 * 4. No other code changes needed
 */
@Service
public class MockCurrencyConversionService implements CurrencyConversionService {
    
    /**
     * Mock exchange rates (against USD as base).
     * 
     * Exchange rates are stored as currency_pair -> rate
     * Example: "USD_EUR" -> 0.92 means 1 USD = 0.92 EUR
     */
    private static final Map<String, BigDecimal> EXCHANGE_RATES = new HashMap<>();
    
    static {
        // Initialize mock exchange rates
        // Base: USD = 1.0
        EXCHANGE_RATES.put("USD_USD", BigDecimal.ONE);
        EXCHANGE_RATES.put("USD_EUR", new BigDecimal("0.92"));
        EXCHANGE_RATES.put("USD_GBP", new BigDecimal("0.79"));
        EXCHANGE_RATES.put("USD_JPY", new BigDecimal("109.50"));
        EXCHANGE_RATES.put("USD_AUD", new BigDecimal("1.35"));
        EXCHANGE_RATES.put("USD_CAD", new BigDecimal("1.25"));
        EXCHANGE_RATES.put("USD_CNY", new BigDecimal("6.45"));
        EXCHANGE_RATES.put("USD_INR", new BigDecimal("74.50"));
        EXCHANGE_RATES.put("USD_MXN", new BigDecimal("20.15"));
        EXCHANGE_RATES.put("USD_CHF", new BigDecimal("0.92"));
        
        // EUR rates
        EXCHANGE_RATES.put("EUR_USD", new BigDecimal("1.09"));
        EXCHANGE_RATES.put("EUR_EUR", BigDecimal.ONE);
        EXCHANGE_RATES.put("EUR_GBP", new BigDecimal("0.86"));
        EXCHANGE_RATES.put("EUR_JPY", new BigDecimal("119.00"));
        EXCHANGE_RATES.put("EUR_AUD", new BigDecimal("1.47"));
        
        // GBP rates
        EXCHANGE_RATES.put("GBP_USD", new BigDecimal("1.27"));
        EXCHANGE_RATES.put("GBP_EUR", new BigDecimal("1.16"));
        EXCHANGE_RATES.put("GBP_GBP", BigDecimal.ONE);
        EXCHANGE_RATES.put("GBP_JPY", new BigDecimal("139.00"));
        EXCHANGE_RATES.put("GBP_AUD", new BigDecimal("1.71"));
        
        // JPY rates
        EXCHANGE_RATES.put("JPY_USD", new BigDecimal("0.0091"));
        EXCHANGE_RATES.put("JPY_EUR", new BigDecimal("0.0084"));
        EXCHANGE_RATES.put("JPY_GBP", new BigDecimal("0.0072"));
        EXCHANGE_RATES.put("JPY_JPY", BigDecimal.ONE);
        EXCHANGE_RATES.put("JPY_AUD", new BigDecimal("0.012"));
        
        // AUD rates
        EXCHANGE_RATES.put("AUD_USD", new BigDecimal("0.74"));
        EXCHANGE_RATES.put("AUD_EUR", new BigDecimal("0.68"));
        EXCHANGE_RATES.put("AUD_GBP", new BigDecimal("0.58"));
        EXCHANGE_RATES.put("AUD_JPY", new BigDecimal("81.00"));
        EXCHANGE_RATES.put("AUD_AUD", BigDecimal.ONE);
        
        // INR rates
        EXCHANGE_RATES.put("INR_USD", new BigDecimal("0.0134"));
        EXCHANGE_RATES.put("INR_EUR", new BigDecimal("0.0123"));
        EXCHANGE_RATES.put("INR_INR", BigDecimal.ONE);
        
        // CAD, CNY, MXN, CHF rates
        EXCHANGE_RATES.put("CAD_USD", new BigDecimal("0.80"));
        EXCHANGE_RATES.put("CAD_CAD", BigDecimal.ONE);
        EXCHANGE_RATES.put("CNY_USD", new BigDecimal("0.155"));
        EXCHANGE_RATES.put("CNY_CNY", BigDecimal.ONE);
        EXCHANGE_RATES.put("MXN_USD", new BigDecimal("0.050"));
        EXCHANGE_RATES.put("MXN_MXN", BigDecimal.ONE);
        EXCHANGE_RATES.put("CHF_USD", new BigDecimal("1.09"));
        EXCHANGE_RATES.put("CHF_CHF", BigDecimal.ONE);
    }
    
    @Override
    public CurrencyConversionResponse convert(CurrencyConversionRequest request) {
        String sourceCurrency = request.sourceCurrency().toUpperCase();
        String destinationCurrency = request.destinationCurrency().toUpperCase();
        BigDecimal amount = request.amount();
        
        // Validate currencies
        if (!isValidCurrency(sourceCurrency) || !isValidCurrency(destinationCurrency)) {
            throw new PaymentValidationException(
                "Invalid currency conversion: " + sourceCurrency + " to " + destinationCurrency,
                "INVALID_CURRENCY_CONVERSION"
            );
        }
        
        // If same currency, return 1:1 conversion
        if (sourceCurrency.equals(destinationCurrency)) {
            return new CurrencyConversionResponse(
                sourceCurrency,
                destinationCurrency,
                amount,
                BigDecimal.ONE,
                amount
            );
        }
        
        // Get exchange rate
        String rateKey = sourceCurrency + "_" + destinationCurrency;
        BigDecimal exchangeRate = EXCHANGE_RATES.get(rateKey);
        
        if (exchangeRate == null) {
            throw new PaymentValidationException(
                "Exchange rate not available: " + rateKey,
                "EXCHANGE_RATE_NOT_AVAILABLE"
            );
        }
        
        // Apply exchange rate and round to 2 decimal places
        BigDecimal convertedAmount = amount.multiply(exchangeRate)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        return new CurrencyConversionResponse(
            sourceCurrency,
            destinationCurrency,
            amount,
            exchangeRate,
            convertedAmount
        );
    }
    
    /**
     * Check if a currency code is supported.
     */
    private boolean isValidCurrency(String currency) {
        // Check if at least one rate exists for this currency
        return EXCHANGE_RATES.keySet().stream()
            .anyMatch(key -> key.startsWith(currency + "_") || key.endsWith("_" + currency));
    }
}

