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

import com.neueda.domain.BulkPaymentBatchRecord;

@ExtendWith(MockitoExtension.class)
class BulkPaymentBatchRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BulkPaymentBatchRepositoryImpl repository;
    private BulkPaymentBatchRecord sampleBatch;

    @BeforeEach
    void setUp() {
        repository = new BulkPaymentBatchRepositoryImpl(jdbcTemplate);
        sampleBatch = BulkPaymentBatchRecord.create("BP123", "KEY1", "111122223333", BigDecimal.valueOf(100), 1, "USER1");
    }

    @Test
    @DisplayName("findById: Success returns batch")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleBatch));

        Optional<BulkPaymentBatchRecord> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("BP123", result.get().batchReference());
    }

    @Test
    @DisplayName("findByReference: Success returns batch")
    void findByReference_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("BP123")))
            .thenReturn(List.of(sampleBatch));

        Optional<BulkPaymentBatchRecord> result = repository.findByReference("BP123");

        assertTrue(result.isPresent());
        assertEquals("BP123", result.get().batchReference());
    }

    @Test
    @DisplayName("findByIdempotencyKey: Success returns batch")
    void findByIdempotencyKey_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("KEY1")))
            .thenReturn(List.of(sampleBatch));

        Optional<BulkPaymentBatchRecord> result = repository.findByIdempotencyKey("KEY1");

        assertTrue(result.isPresent());
        assertEquals("KEY1", result.get().idempotencyKey());
    }
}
