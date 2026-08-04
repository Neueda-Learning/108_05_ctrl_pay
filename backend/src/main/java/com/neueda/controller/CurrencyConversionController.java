package com.neueda.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.service.CurrencyConversionService;

import jakarta.validation.Valid;

/**
 * REST Controller for currency conversion operations.
 * 
 * Endpoints:
 * - POST /api/convert - Convert amount between currencies
 * 
 * This controller uses an abstracted CurrencyConversionService.
 * The service can be easily replaced from mock to real implementation
 * without any changes to this controller.
 */
@RestController
@RequestMapping("/api")
public class CurrencyConversionController {
    
    private final CurrencyConversionService conversionService;
    
    public CurrencyConversionController(CurrencyConversionService conversionService) {
        this.conversionService = conversionService;
    }
    
    /**
     * Convert an amount from one currency to another.
     * 
     * Request: POST /api/convert
     * Body: CurrencyConversionRequest
     * 
     * Example Request:
     * {
     *   "amount": 100.00,
     *   "sourceCurrency": "USD",
     *   "destinationCurrency": "EUR"
     * }
     * 
     * Example Response:
     * {
     *   "sourceCurrency": "USD",
     *   "destinationCurrency": "EUR",
     *   "sourceAmount": 100.00,
     *   "exchangeRate": 0.92,
     *   "convertedAmount": 92.00
     * }
     * 
     * Response:
     * - 200 OK: Conversion successful
     * - 400 Bad Request: Validation error (invalid currencies, amounts, etc.)
     * - 500 Internal Server Error: Server error
     * 
     * @param request conversion request with amount and currencies
     * @return 200 OK with conversion result
     */
    @PostMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convert(
        @Valid @RequestBody CurrencyConversionRequest request
    ) {
        try {
            CurrencyConversionResponse response = conversionService.convert(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new PaymentProcessingException("Currency conversion failed: " + e.getMessage());
        }
    }
}

