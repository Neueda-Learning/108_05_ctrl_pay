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

import com.neueda.domain.MLModelPredictionRecord;

@ExtendWith(MockitoExtension.class)
class JdbcMLModelPredictionRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcMLModelPredictionRepositoryImpl repository;
    private MLModelPredictionRecord samplePrediction;

    @BeforeEach
    void setUp() {
        repository = new JdbcMLModelPredictionRepositoryImpl(jdbcTemplate);
        samplePrediction = MLModelPredictionRecord.create(
            1L, 100L, 10L, BigDecimal.valueOf(85), BigDecimal.valueOf(90), 12
        );
    }

    @Test
    @DisplayName("findByPaymentId: Returns predictions for payment")
    void findByPaymentId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(100L)))
            .thenReturn(List.of(samplePrediction));

        List<MLModelPredictionRecord> predictions = repository.findByPaymentId(100L);

        assertNotNull(predictions);
        assertEquals(1, predictions.size());
    }

    @Test
    @DisplayName("findById: Returns prediction optional")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(samplePrediction));

        Optional<MLModelPredictionRecord> prediction = repository.findById(1L);

        assertTrue(prediction.isPresent());
    }

    @Test
    @DisplayName("countByModelId: Returns prediction count")
    void countByModelId_Success() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L)))
            .thenReturn(10L);

        long count = repository.countByModelId(1L);

        assertEquals(10L, count);
    }
}
