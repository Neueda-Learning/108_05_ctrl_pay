package com.neueda.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.FraudRuleRecord;
import com.neueda.dto.CreateFraudRuleRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.FraudRuleService;

@ExtendWith(MockitoExtension.class)
class FraudRuleAdminControllerTest {

    @Mock
    private FraudRuleService fraudRuleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private FraudRuleRecord sampleRule;

    @BeforeEach
    void setUp() {
        FraudRuleAdminController controller = new FraudRuleAdminController(fraudRuleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        sampleRule = FraudRuleRecord.create(
            "LARGE_TX", "AMOUNT", "Large TX", "HIGH", 1, BigDecimal.ONE, "{}", "{}"
        );
    }

    @Test
    @DisplayName("GET /api/admin/fraud-rules: Success list rules")
    void listRules_Success() throws Exception {
        when(fraudRuleService.getAllRules()).thenReturn(List.of(sampleRule));
        when(fraudRuleService.getActiveRuleCount()).thenReturn(1L);

        mockMvc.perform(get("/api/admin/fraud-rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.activeCount").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/fraud-rules/{id}: Success get details")
    void getRule_Success() throws Exception {
        when(fraudRuleService.getRule(1L)).thenReturn(Optional.of(sampleRule));

        mockMvc.perform(get("/api/admin/fraud-rules/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruleName").value("LARGE_TX"));
    }

    @Test
    @DisplayName("POST /api/admin/fraud-rules: Success create rule")
    void createRule_Success() throws Exception {
        when(fraudRuleService.createRule(any())).thenReturn(sampleRule);

        CreateFraudRuleRequest req = new CreateFraudRuleRequest(
            "LARGE_TX", "AMOUNT", "Large TX", "HIGH", 1, BigDecimal.ONE, "{}", "{}"
        );

        mockMvc.perform(post("/api/admin/fraud-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ruleName").value("LARGE_TX"));
    }

    @Test
    @DisplayName("PUT /api/admin/fraud-rules/{id}: Success update rule")
    void updateRule_Success() throws Exception {
        when(fraudRuleService.updateRule(eq(1L), any())).thenReturn(sampleRule);

        CreateFraudRuleRequest req = new CreateFraudRuleRequest(
            "LARGE_TX", "AMOUNT", "Updated", "HIGH", 1, BigDecimal.ONE, "{}", "{}"
        );

        mockMvc.perform(put("/api/admin/fraud-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/admin/fraud-rules/{id}/toggle: Success toggle rule status")
    void toggleRule_Success() throws Exception {
        doNothing().when(fraudRuleService).toggleRuleStatus(1L);
        when(fraudRuleService.getRule(1L)).thenReturn(Optional.of(sampleRule));

        mockMvc.perform(patch("/api/admin/fraud-rules/1/toggle"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Rule toggled successfully"));
    }

    @Test
    @DisplayName("DELETE /api/admin/fraud-rules/{id}: Success delete rule")
    void deleteRule_Success() throws Exception {
        doNothing().when(fraudRuleService).deleteRule(1L);

        mockMvc.perform(delete("/api/admin/fraud-rules/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Fraud rule deleted successfully"));
    }
}
