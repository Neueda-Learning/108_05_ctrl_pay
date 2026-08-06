package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

import com.neueda.dto.DashboardOverviewDTO;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.AnalyticsService;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock private AnalyticsService analyticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnalyticsController controller = new AnalyticsController(analyticsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/dashboard/overview: Returns dashboard overview statistics")
    void getDashboardOverview_Success() throws Exception {
        DashboardOverviewDTO overview = new DashboardOverviewDTO(
            100L, BigDecimal.valueOf(10000), 95L, 3L, 2L, 95.0, 3L, 50L, 80L, "OK", LocalDateTime.now()
        );

        when(analyticsService.getDashboardOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/dashboard/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTransactions").value(100))
            .andExpect(jsonPath("$.transactionSuccessRate").value(95.0))
            .andExpect(jsonPath("$.systemHealth").value("OK"));
    }
}
