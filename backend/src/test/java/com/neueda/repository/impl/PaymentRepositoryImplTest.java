package com.neueda.repository.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PaymentRepositoryImpl repository;
    private PaymentRecord samplePayment;

    @BeforeEach
    void setUp() {
        repository = new PaymentRepositoryImpl(jdbcTemplate);

        samplePayment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("findById: Success returns payment record")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(samplePayment));

        Optional<PaymentRecord> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
    }

    @Test
    @DisplayName("findById: Returns empty optional when not found")
    void findById_NotFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99L)))
            .thenReturn(List.of());

        Optional<PaymentRecord> result = repository.findById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByIdempotencyKey: Success returns payment record")
    void findByIdempotencyKey_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("KEY1")))
            .thenReturn(List.of(samplePayment));

        Optional<PaymentRecord> result = repository.findByIdempotencyKey("KEY1");

        assertTrue(result.isPresent());
        assertEquals("KEY1", result.get().idempotencyKey());
    }

    @Test
    @DisplayName("findAll: Returns list of payments")
    void findAll_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(List.of(samplePayment));

        List<PaymentRecord> payments = repository.findAll();

        assertNotNull(payments);
        assertEquals(1, payments.size());
    }
}
