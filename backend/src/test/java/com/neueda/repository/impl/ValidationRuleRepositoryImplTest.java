package com.neueda.repository.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neueda.domain.RuleType;
import com.neueda.domain.Severity;
import com.neueda.domain.ValidationRuleRecord;

@ExtendWith(MockitoExtension.class)
class ValidationRuleRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private ValidationRuleRepositoryImpl repository;
    private ValidationRuleRecord sampleRule;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new ValidationRuleRepositoryImpl(jdbcTemplate, objectMapper);

        ObjectNode ruleDef = objectMapper.createObjectNode();
        ruleDef.put("min", 1);

        sampleRule = new ValidationRuleRecord(
            1L, "AMOUNT_RANGE", "Desc", RuleType.AMOUNT_RANGE,
            ruleDef, true, Severity.HARD, 1, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("findById: Success returns rule")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleRule));

        Optional<ValidationRuleRecord> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("AMOUNT_RANGE", result.get().name());
    }

    @Test
    @DisplayName("findActiveRules: Returns active rules")
    void findActiveRules_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(List.of(sampleRule));

        List<ValidationRuleRecord> active = repository.findActiveRules();

        assertNotNull(active);
        assertEquals(1, active.size());
    }
}
