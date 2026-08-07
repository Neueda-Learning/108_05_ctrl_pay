package com.neueda.repository.impl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.neueda.domain.ValidationResultRecord;

@ExtendWith(MockitoExtension.class)
class ValidationResultRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper;
    private ValidationResultRepositoryImpl repository;
    private ValidationResultRecord sampleResult;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new ValidationResultRepositoryImpl(jdbcTemplate, objectMapper);
        sampleResult = ValidationResultRecord.success(1L, 10L, "AMOUNT_RANGE", objectMapper.createObjectNode(), 5);
    }

    @Test
    @DisplayName("findByPaymentId: Returns results for payment")
    void findByPaymentId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleResult));

        List<ValidationResultRecord> results = repository.findByPaymentId(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("findByValidationRuleId: Returns results for rule")
    void findByValidationRuleId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10L)))
            .thenReturn(List.of(sampleResult));

        List<ValidationResultRecord> results = repository.findByValidationRuleId(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
    }
}
