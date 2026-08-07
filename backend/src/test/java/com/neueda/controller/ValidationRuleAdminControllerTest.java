package com.neueda.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neueda.domain.RuleType;
import com.neueda.domain.Severity;
import com.neueda.domain.ValidationRuleRecord;
import com.neueda.dto.CreateValidationRuleRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.repository.ValidationRuleRepository;
import com.neueda.validation.RuleEngine;
import com.neueda.validation.RuleFactory;

@ExtendWith(MockitoExtension.class)
class ValidationRuleAdminControllerTest {

    @Mock
    private ValidationRuleRepository validationRuleRepository;
    @Mock
    private RuleEngine ruleEngine;
    @Mock
    private RuleFactory ruleFactory;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ValidationRuleRecord sampleRule;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ValidationRuleAdminController controller = new ValidationRuleAdminController(
            validationRuleRepository, ruleEngine, ruleFactory, objectMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("min_amount", 1);

        sampleRule = new ValidationRuleRecord(
            1L, "AMOUNT_RANGE", "Check min max amount", RuleType.AMOUNT_RANGE,
            ruleDef, true, Severity.HARD, 1, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("GET /api/admin/validation-rules: Success list rules")
    void listRules_Success() throws Exception {
        when(validationRuleRepository.findAll()).thenReturn(List.of(sampleRule));

        mockMvc.perform(get("/api/admin/validation-rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("AMOUNT_RANGE"));
    }

    @Test
    @DisplayName("GET /api/admin/validation-rules/{id}: Success get details")
    void getRule_Success() throws Exception {
        when(validationRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        mockMvc.perform(get("/api/admin/validation-rules/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("AMOUNT_RANGE"));
    }

    @Test
    @DisplayName("POST /api/admin/validation-rules: Success create rule")
    void createRule_Success() throws Exception {
        when(validationRuleRepository.save(any())).thenReturn(sampleRule);

        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("min_amount", 1);

        CreateValidationRuleRequest req = new CreateValidationRuleRequest(
            "AMOUNT_RANGE", "Check min max amount", "AMOUNT_RANGE",
            ruleDef, "HARD", 1
        );

        mockMvc.perform(post("/api/admin/validation-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("AMOUNT_RANGE"));
    }

    @Test
    @DisplayName("PUT /api/admin/validation-rules/{id}: Success update rule")
    void updateRule_Success() throws Exception {
        when(validationRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(validationRuleRepository.update(any())).thenReturn(sampleRule);

        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("min_amount", 10);

        CreateValidationRuleRequest req = new CreateValidationRuleRequest(
            "AMOUNT_RANGE", "Updated desc", "AMOUNT_RANGE",
            ruleDef, "HARD", 1
        );

        mockMvc.perform(put("/api/admin/validation-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/admin/validation-rules/{id}/toggle: Success toggle active status")
    void toggleRule_Success() throws Exception {
        when(validationRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(validationRuleRepository.update(any())).thenReturn(sampleRule);

        mockMvc.perform(patch("/api/admin/validation-rules/1/toggle"))
            .andExpect(status().isOk());
    }
}
