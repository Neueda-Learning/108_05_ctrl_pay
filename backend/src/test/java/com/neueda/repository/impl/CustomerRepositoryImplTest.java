package com.neueda.repository.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;

@ExtendWith(MockitoExtension.class)
class CustomerRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CustomerRepositoryImpl repository;
    private CustomerRecord sampleCustomer;

    @BeforeEach
    void setUp() {
        repository = new CustomerRepositoryImpl(jdbcTemplate);
        sampleCustomer = new CustomerRecord(
            101L, "John Doe", LocalDate.of(1990, 1, 1), "+1234567890",
            "ABCDE1234F", LocalDateTime.now(), LocalDateTime.now(), "USA", CustomerStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("findById: Success returns customer")
    void findById_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(101L)))
            .thenReturn(List.of(sampleCustomer));

        Optional<CustomerRecord> result = repository.findById(101L);

        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().name());
    }

    @Test
    @DisplayName("findByPanNumber: Success returns customer")
    void findByPanNumber_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("ABCDE1234F")))
            .thenReturn(List.of(sampleCustomer));

        Optional<CustomerRecord> result = repository.findByPanNumber("ABCDE1234F");

        assertTrue(result.isPresent());
        assertEquals("ABCDE1234F", result.get().panNumber());
    }

    @Test
    @DisplayName("existsByPanNumber: Returns true when customer exists")
    void existsByPanNumber_True() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("ABCDE1234F")))
            .thenReturn(1L);

        boolean exists = repository.existsByPanNumber("ABCDE1234F");

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByPanNumber: Returns false when customer does not exist")
    void existsByPanNumber_False() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("INVALID")))
            .thenReturn(0L);

        boolean exists = repository.existsByPanNumber("INVALID");

        assertFalse(exists);
    }
}
