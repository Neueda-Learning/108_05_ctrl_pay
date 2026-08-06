package com.neueda.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.CurrencyConversionService;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionControllerTest {

    @Mock private CurrencyConversionService currencyConversionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CurrencyConversionController controller = new CurrencyConversionController(currencyConversionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/convert: Converts amount between currencies")
    void convertCurrency_Success() throws Exception {
        CurrencyConversionRequest req = new CurrencyConversionRequest(BigDecimal.valueOf(100), "USD", "EUR");
        CurrencyConversionResponse resp = new CurrencyConversionResponse("USD", "EUR", BigDecimal.valueOf(100), BigDecimal.valueOf(0.85), BigDecimal.valueOf(85));

        when(currencyConversionService.convert(any())).thenReturn(resp);

        mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exchangeRate").value(0.85))
            .andExpect(jsonPath("$.convertedAmount").value(85));
    }
}
