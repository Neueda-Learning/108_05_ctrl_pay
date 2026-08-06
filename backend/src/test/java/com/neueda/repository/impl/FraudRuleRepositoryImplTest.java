package com.neueda.repository.impl;

import java.math.BigDecimal;
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

import com.neueda.domain.FraudRuleRecord;

@ExtendWith(MockitoExtension.class)
class FraudRuleRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private FraudRuleRepositoryImpl repository;
    private FraudRuleRecord sampleRule;

    @BeforeEach
    void setUp() {
        repository = new FraudRuleRepositoryImpl(jdbcTemplate);
        sampleRule = FraudRuleRecord.create(
            "LARGE_TX", "AMOUNT", "Large TX", "HIGH", 1, BigDecimal.ONE, "{}", "{}"
        );
    }

    @Test
    @DisplayName("findById: Success returns rule")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleRule));

        Optional<FraudRuleRecord> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("LARGE_TX", result.get().ruleName());
    }

    @Test
    @DisplayName("findByName: Success returns rule")
    void findByName_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("LARGE_TX")))
            .thenReturn(List.of(sampleRule));

        Optional<FraudRuleRecord> result = repository.findByName("LARGE_TX");

        assertTrue(result.isPresent());
        assertEquals("LARGE_TX", result.get().ruleName());
    }

    @Test
    @DisplayName("findAllActive: Returns active rules")
    void findAllActive_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(List.of(sampleRule));

        List<FraudRuleRecord> active = repository.findAllActive();

        assertNotNull(active);
        assertEquals(1, active.size());
    }

    @Test
    @DisplayName("countActive: Returns active rule count")
    void countActive_Success() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(5);

        long count = repository.countActive();

        assertEquals(5L, count);
    }
}
