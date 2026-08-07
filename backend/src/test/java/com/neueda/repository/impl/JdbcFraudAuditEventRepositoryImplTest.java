package com.neueda.repository.impl;

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

import com.neueda.domain.FraudAuditEventRecord;

@ExtendWith(MockitoExtension.class)
class JdbcFraudAuditEventRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcFraudAuditEventRepositoryImpl repository;
    private FraudAuditEventRecord sampleEvent;

    @BeforeEach
    void setUp() {
        repository = new JdbcFraudAuditEventRepositoryImpl(jdbcTemplate);
        sampleEvent = FraudAuditEventRecord.create(100L, "RULE_TRIGGERED", "SYSTEM", "{}");
    }

    @Test
    @DisplayName("findByAssessmentId: Returns audit events")
    void findByAssessmentId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(100L)))
            .thenReturn(List.of(sampleEvent));

        List<FraudAuditEventRecord> events = repository.findByAssessmentId(100L);

        assertNotNull(events);
        assertEquals(1, events.size());
    }

    @Test
    @DisplayName("findById: Returns audit event")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(List.of(sampleEvent));

        Optional<FraudAuditEventRecord> event = repository.findById(1L);

        assertTrue(event.isPresent());
    }

    @Test
    @DisplayName("countByEventType: Returns count")
    void countByEventType_Success() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("RULE_TRIGGERED")))
            .thenReturn(5L);

        long count = repository.countByEventType("RULE_TRIGGERED");

        assertEquals(5L, count);
    }
}
