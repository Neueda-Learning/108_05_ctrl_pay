package com.neueda.repository.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AccountRepositoryImpl repository;

    private AccountRecord sampleAccount;

    @BeforeEach
    void setUp() {
        repository = new AccountRepositoryImpl(jdbcTemplate);

        sampleAccount = new AccountRecord(
            1L, 101L, "ACC123", "John Doe Account", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC001", "NYC", "Global Bank", "1234"
        );
    }

    @Test
    @DisplayName("findByAccountNumber: Success returns account record")
    void findByAccountNumber_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("ACC123")))
            .thenReturn(List.of(sampleAccount));

        Optional<AccountRecord> result = repository.findByAccountNumber("ACC123");

        assertTrue(result.isPresent());
        assertEquals("ACC123", result.get().accountNumber());
    }

    @Test
    @DisplayName("findByAccountNumber: Returns empty optional when not found")
    void findByAccountNumber_NotFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("INVALID")))
            .thenReturn(List.of());

        Optional<AccountRecord> result = repository.findByAccountNumber("INVALID");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByCustomerId: Success returns accounts list")
    void findByCustomerId_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(101L)))
            .thenReturn(List.of(sampleAccount));

        List<AccountRecord> accounts = repository.findByCustomerId(101L);

        assertNotNull(accounts);
        assertEquals(1, accounts.size());
    }
}
