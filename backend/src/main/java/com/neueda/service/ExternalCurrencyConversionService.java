package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.exception.PaymentValidationException;

/**
 * Real implementation of CurrencyConversionService.
 * 
 * Calls external currency API for real exchange rates.
 * Caches exchange rates for a configurable duration (default: 1 hour).
 * 
 * External API: https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{currency}.json
 * 
 * Example: GET https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.json
 * Response:
 * {
 *   "date": "2026-08-03",
 *   "eur": {
 *     "inr": 109.7168,
 *     "usd": 1.1529
 *   }
 * }
 */
@Service
@Primary
public class ExternalCurrencyConversionService implements CurrencyConversionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalCurrencyConversionService.class);
    private static final String CURRENCY_API_BASE_URL = 
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies";
    
    /**
     * Cache entry for exchange rates.
     * Stores timestamp to implement expiration logic.
     */
    private static class CacheEntry {
        BigDecimal rate;
        LocalDateTime timestamp;
        
        CacheEntry(BigDecimal rate) {
            this.rate = rate;
            this.timestamp = LocalDateTime.now();
        }
        
        boolean isExpired(long cacheDurationMinutes) {
            return LocalDateTime.now().minusMinutes(cacheDurationMinutes).isAfter(timestamp);
        }
    }
    
    /**
     * Cache for exchange rates.
     * Key format: "SOURCE_DESTINATION" (e.g., "EUR_INR")
     * Value: CacheEntry with rate and timestamp
     */
    private final Map<String, CacheEntry> rateCache = new ConcurrentHashMap<>();
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CurrencyService currencyService;
    
    /**
     * Cache validity duration in minutes.
     * Exchange rates are cached for 60 minutes to reduce API calls.
     */
    private static final long CACHE_DURATION_MINUTES = 60;
    
    public ExternalCurrencyConversionService(
        RestTemplate restTemplate, 
        ObjectMapper objectMapper,
        CurrencyService currencyService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.currencyService = currencyService;
    }
    
    @Override
    public CurrencyConversionResponse convert(CurrencyConversionRequest request) {
        String sourceCurrency = request.sourceCurrency().toUpperCase();
        String destinationCurrency = request.destinationCurrency().toUpperCase();
        BigDecimal amount = request.amount();
        
        // Validate currencies are supported
        if (!currencyService.isValidCurrency(sourceCurrency)) {
            throw new PaymentValidationException(
                "Source currency not supported: " + sourceCurrency,
                "INVALID_CURRENCY_CONVERSION"
            );
        }
        
        if (!currencyService.isValidCurrency(destinationCurrency)) {
            throw new PaymentValidationException(
                "Destination currency not supported: " + destinationCurrency,
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
        BigDecimal exchangeRate = getExchangeRate(sourceCurrency, destinationCurrency);
        
        // Calculate converted amount
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
     * Get exchange rate from source currency to destination currency.
     * Implements caching logic to reduce external API calls.
     * 
     * @param sourceCurrency source currency code (uppercase)
     * @param destinationCurrency destination currency code (uppercase)
     * @return exchange rate (1 source unit = ? destination units)
     */
    private BigDecimal getExchangeRate(String sourceCurrency, String destinationCurrency) {
        String cacheKey = sourceCurrency + "_" + destinationCurrency;
        
        // Check cache first
        CacheEntry cached = rateCache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_DURATION_MINUTES)) {
            logger.debug("Using cached rate for {}: {}", cacheKey, cached.rate);
            return cached.rate;
        }
        
        // Fetch from external API
        try {
            BigDecimal rate = fetchExchangeRateFromAPI(sourceCurrency, destinationCurrency);
            
            // Cache the rate
            rateCache.put(cacheKey, new CacheEntry(rate));
            
            logger.info("Fetched exchange rate for {}: {}", cacheKey, rate);
            return rate;
            
        } catch (Exception e) {
            logger.error("Failed to fetch exchange rate for {}", cacheKey, e);
            throw new PaymentValidationException(
                "Exchange rate not available for " + cacheKey + ": " + e.getMessage(),
                "CURRENCY_CONVERSION_FAILED"
            );
        }
    }
    
    /**
     * Fetch exchange rate from external API.
     * 
     * Calls: GET https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{sourceCurrency}.json
     * 
     * @param sourceCurrency source currency code (lowercase)
     * @param destinationCurrency destination currency code (lowercase)
     * @return exchange rate
     */
    private BigDecimal fetchExchangeRateFromAPI(String sourceCurrency, String destinationCurrency) {
        try {
            // Construct API URL - source currency in lowercase
            String apiUrl = String.format(
                "%s/%s.json",
                CURRENCY_API_BASE_URL,
                sourceCurrency.toLowerCase()
            );
            
            logger.debug("Fetching exchange rate from: {}", apiUrl);
            
            // Call external API
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            // Parse JSON response
            JsonNode root = objectMapper.readTree(response);
            
            // Navigate JSON: { "currencyCode": { "targetCurrency": rate } }
            JsonNode currencyNode = root.get(sourceCurrency.toLowerCase());
            if (currencyNode == null) {
                throw new PaymentValidationException(
                    "Currency data not found in API response for " + sourceCurrency,
                    "CURRENCY_CONVERSION_FAILED"
                );
            }
            
            JsonNode rateNode = currencyNode.get(destinationCurrency.toLowerCase());
            if (rateNode == null) {
                throw new PaymentValidationException(
                    "Exchange rate not available: " + sourceCurrency + " to " + destinationCurrency,
                    "CURRENCY_CONVERSION_FAILED"
                );
            }
            
            BigDecimal rate = new BigDecimal(rateNode.asText());
            return rate;
            
        } catch (PaymentValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentValidationException(
                "Failed to fetch exchange rate from external API: " + e.getMessage(),
                "CURRENCY_SERVICE_UNAVAILABLE"
            );
        }
    }
    
    /**
     * Clear exchange rate cache.
     * Useful for testing or manual cache invalidation.
     */
    public void clearCache() {
        rateCache.clear();
        logger.info("Exchange rate cache cleared");
    }
}

