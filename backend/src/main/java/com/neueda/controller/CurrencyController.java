package com.neueda.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.dto.CurrencyResponse;
import com.neueda.service.CurrencyService;

/**
 * REST Controller for currency management operations.
 * 
 * Endpoints:
 * - GET /api/currencies - Get list of all available currencies
 * 
 * This controller provides access to dynamically loaded currency data.
 * Currencies are loaded from an external API and cached in memory.
 */
@RestController
@RequestMapping("/api")
public class CurrencyController {
    
    private final CurrencyService currencyService;
    
    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }
    
    /**
     * Get list of all available currencies.
     * 
     * Request: GET /api/currencies
     * 
     * Example Response:
     * [
     *   {
     *     "code": "EUR",
     *     "name": "Euro"
     *   },
     *   {
     *     "code": "USD",
     *     "name": "US Dollar"
     *   },
     *   {
     *     "code": "INR",
     *     "name": "Indian Rupee"
     *   }
     * ]
     * 
     * Response:
     * - 200 OK: List of available currencies
     * - 503 Service Unavailable: Currency service is not available
     * 
     * @return 200 OK with list of currencies
     */
    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencyResponse>> getAllCurrencies() {
        List<CurrencyResponse> currencies = currencyService.getAllCurrencies();
        return ResponseEntity.ok(currencies);
    }
}

