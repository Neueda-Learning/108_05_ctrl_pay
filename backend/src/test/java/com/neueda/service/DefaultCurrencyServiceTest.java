package com.neueda.service;

import java.util.List;

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
import com.neueda.dto.CurrencyResponse;
import com.neueda.exception.PaymentProcessingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DefaultCurrencyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private DefaultCurrencyService currencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        currencyService = new DefaultCurrencyService(restTemplate, objectMapper);
    }

    @Test
    @DisplayName("refreshCurrencies: Should fetch and filter valid 3-letter currency codes")
    void refreshCurrencies_Success() {
        String jsonResponse = "{\"usd\":\"US Dollar\",\"eur\":\"Euro\",\"invalidcode\":\"Invalid Currency\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);

        currencyService.refreshCurrencies();

        List<CurrencyResponse> currencies = currencyService.getAllCurrencies();
        assertThat(currencies).hasSize(2);
        assertThat(currencyService.isValidCurrency("USD")).isTrue();
        assertThat(currencyService.isValidCurrency("EUR")).isTrue();
        assertThat(currencyService.isValidCurrency("INVALIDCODE")).isFalse();
        assertThat(currencyService.isValidCurrency(null)).isFalse();
    }

    @Test
    @DisplayName("refreshCurrencies: Should throw PaymentProcessingException when API fails")
    void refreshCurrencies_Failure() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("API down"));

        assertThatThrownBy(() -> currencyService.refreshCurrencies())
            .isInstanceOf(PaymentProcessingException.class)
            .hasMessageContaining("Failed to load currencies from external API");
    }

    @Test
    @DisplayName("loadCurrenciesOnStartup: Should handle exceptions gracefully on startup")
    void loadCurrenciesOnStartup_ExceptionHandled() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("Network error"));

        // Should not throw exception
        currencyService.loadCurrenciesOnStartup();
        assertThat(currencyService.isValidCurrency("USD")).isFalse();
    }
}
