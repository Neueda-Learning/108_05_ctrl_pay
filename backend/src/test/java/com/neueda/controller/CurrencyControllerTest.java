package com.neueda.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neueda.dto.CurrencyResponse;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.CurrencyService;

@ExtendWith(MockitoExtension.class)
class CurrencyControllerTest {

    @Mock private CurrencyService currencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CurrencyController controller = new CurrencyController(currencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/currencies: Returns list of supported currencies")
    void getAllCurrencies_Success() throws Exception {
        when(currencyService.getAllCurrencies()).thenReturn(List.of(
            new CurrencyResponse("USD", "US Dollar"),
            new CurrencyResponse("EUR", "Euro")
        ));

        mockMvc.perform(get("/api/currencies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("USD"))
            .andExpect(jsonPath("$[1].code").value("EUR"));
    }
}
