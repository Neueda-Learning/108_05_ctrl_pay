package com.neueda.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.exception.PaymentValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ExternalCurrencyConversionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CurrencyService currencyService;

    private ObjectMapper objectMapper;
    private ExternalCurrencyConversionService conversionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        conversionService = new ExternalCurrencyConversionService(restTemplate, objectMapper, currencyService);
        conversionService.clearCache();
    }

    @Test
    @DisplayName("convert: Same currency returns 1:1 rate without API call")
    void convert_SameCurrency() {
        when(currencyService.isValidCurrency("USD")).thenReturn(true);

        CurrencyConversionRequest req = new CurrencyConversionRequest(BigDecimal.valueOf(100), "USD", "USD");
        CurrencyConversionResponse resp = conversionService.convert(req);

        assertThat(resp.exchangeRate()).isEqualTo(BigDecimal.ONE);
        assertThat(resp.convertedAmount()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("convert: Valid conversion calculates converted amount and caches rate")
    void convert_Success() {
        when(currencyService.isValidCurrency("USD")).thenReturn(true);
        when(currencyService.isValidCurrency("EUR")).thenReturn(true);

        String jsonResponse = "{\"usd\": {\"eur\": 0.85}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);

        CurrencyConversionRequest req = new CurrencyConversionRequest(BigDecimal.valueOf(100), "USD", "EUR");
        CurrencyConversionResponse resp = conversionService.convert(req);

        assertThat(resp.exchangeRate()).isEqualTo(new BigDecimal("0.85"));
        assertThat(resp.convertedAmount()).isEqualTo(new BigDecimal("85.00"));

        // Call convert again -> should use cached rate
        CurrencyConversionResponse respCached = conversionService.convert(req);
        assertThat(respCached.exchangeRate()).isEqualTo(new BigDecimal("0.85"));
    }

    @Test
    @DisplayName("convert: Invalid source currency throws PaymentValidationException")
    void convert_InvalidSourceCurrency() {
        when(currencyService.isValidCurrency("INVALID")).thenReturn(false);

        CurrencyConversionRequest req = new CurrencyConversionRequest(BigDecimal.valueOf(100), "INVALID", "EUR");

        assertThatThrownBy(() -> conversionService.convert(req))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("Source currency not supported");
    }

    @Test
    @DisplayName("convert: External API error throws PaymentValidationException")
    void convert_ApiError() {
        when(currencyService.isValidCurrency("USD")).thenReturn(true);
        when(currencyService.isValidCurrency("EUR")).thenReturn(true);

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("API Timeout"));

        CurrencyConversionRequest req = new CurrencyConversionRequest(BigDecimal.valueOf(100), "USD", "EUR");

        assertThatThrownBy(() -> conversionService.convert(req))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessageContaining("Exchange rate not available");
    }
}
