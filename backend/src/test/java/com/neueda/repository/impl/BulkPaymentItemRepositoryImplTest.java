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

import com.neueda.domain.BulkPaymentItemRecord;

@ExtendWith(MockitoExtension.class)
class BulkPaymentItemRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BulkPaymentItemRepositoryImpl repository;
    private BulkPaymentItemRecord sampleItem;

    @BeforeEach
    void setUp() {
        repository = new BulkPaymentItemRepositoryImpl(jdbcTemplate);
        sampleItem = BulkPaymentItemRecord.create(1L, 1, "444455556666", BigDecimal.valueOf(100), "USD", "Transfer");
    }

    @Test
    @DisplayName("findByBatchId: Returns items for batch")
    void findByBatchId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleItem));

        List<BulkPaymentItemRecord> items = repository.findByBatchId(1L);

        assertNotNull(items);
        assertEquals(1, items.size());
    }

    @Test
    @DisplayName("findById: Returns item optional")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleItem));

        Optional<BulkPaymentItemRecord> item = repository.findById(1L);

        assertTrue(item.isPresent());
    }
}
