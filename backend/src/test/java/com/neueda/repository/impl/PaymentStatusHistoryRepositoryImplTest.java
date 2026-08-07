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

import com.neueda.domain.PaymentStatus;
import com.neueda.domain.PaymentStatusHistoryRecord;

@ExtendWith(MockitoExtension.class)
class PaymentStatusHistoryRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PaymentStatusHistoryRepositoryImpl repository;
    private PaymentStatusHistoryRecord sampleHistory;

    @BeforeEach
    void setUp() {
        repository = new PaymentStatusHistoryRepositoryImpl(jdbcTemplate);
        sampleHistory = PaymentStatusHistoryRecord.transition(
            100L, PaymentStatus.CREATED, PaymentStatus.VALIDATED, "SYSTEM"
        );
    }

    @Test
    @DisplayName("findByPaymentId: Returns status history for payment")
    void findByPaymentId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(100L)))
            .thenReturn(List.of(sampleHistory));

        List<PaymentStatusHistoryRecord> history = repository.findByPaymentId(100L);

        assertNotNull(history);
        assertEquals(1, history.size());
    }
}
