package com.neueda.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.dto.CurrencyResponse;
import com.neueda.exception.PaymentProcessingException;

/**
 * Implementation of CurrencyService.
 * 
 * Loads currencies from external API during application startup.
 * Caches them in memory to avoid repeated API calls.
 * 
 * External API: https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.json
 * 
 * Response format:
 * {
 *   "eur": "Euro",
 *   "usd": "US Dollar",
 *   "inr": "Indian Rupee"
 * }
 */
@Service
public class DefaultCurrencyService implements CurrencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCurrencyService.class);
    private static final String CURRENCIES_API_URL = 
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.json";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * In-memory cache of available currencies.
     * Map<currencyCode, currencyName>
     */
    private Map<String, CurrencyResponse> currenciesCache = new ConcurrentHashMap<>();
    
    public DefaultCurrencyService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Load currencies from external API on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadCurrenciesOnStartup() {
        try {
            logger.info("Loading currencies from external API on application startup...");
            refreshCurrencies();
            logger.info("Successfully loaded {} currencies", currenciesCache.size());
        } catch (Exception e) {
            logger.error("Failed to load currencies on startup. Application will continue but currency features may be unavailable", e);
        }
    }
    
    @Override
    public List<CurrencyResponse> getAllCurrencies() {
        if (currenciesCache.isEmpty()) {
            try {
                refreshCurrencies();
            } catch (PaymentProcessingException e) {
                logger.error("Failed to load currencies", e);
                throw e;
            } catch (Exception e) {
                logger.error("Failed to load currencies", e);
                throw new PaymentProcessingException(
                    "Currency service unavailable: " + e.getMessage(),
                    e
                );
            }
        }
        return new ArrayList<>(currenciesCache.values());
    }
    
    @Override
    public boolean isValidCurrency(String currencyCode) {
        if (currencyCode == null) {
            return false;
        }
        return currenciesCache.containsKey(currencyCode.toUpperCase());
    }
    
    @Override
    public synchronized void refreshCurrencies() {
        try {
            logger.info("Fetching currencies from {}", CURRENCIES_API_URL);
            
            // Call external API
            String response = restTemplate.getForObject(CURRENCIES_API_URL, String.class);
            
            // Parse JSON response
            @SuppressWarnings("unchecked")
            Map<String, String> currencyMap = objectMapper.readValue(response, Map.class);
            
            // Clear cache
            currenciesCache.clear();
            
            // Process currencies: filter out empty/null names and convert to uppercase codes
            for (Map.Entry<String, String> entry : currencyMap.entrySet()) {
                String code = entry.getKey();
                String name = entry.getValue();
                
                // Filter out empty or null currency names
                if (name != null && !name.trim().isEmpty()) {
                    String uppercaseCode = code.toUpperCase();
                    
                    // Only keep valid ISO 4217 codes (3 uppercase letters)
                    if (uppercaseCode.matches("^[A-Z]{3}$")) {
                        currenciesCache.put(
                            uppercaseCode,
                            new CurrencyResponse(uppercaseCode, name)
                        );
                    }
                }
            }
            
            logger.info("Loaded {} valid currencies from external API", currenciesCache.size());
            
        } catch (Exception e) {
            logger.error("Error loading currencies from external API", e);
            throw new PaymentProcessingException(
                "Failed to load currencies from external API: " + e.getMessage()
            );
        }
    }
}




