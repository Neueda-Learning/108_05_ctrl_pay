package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.ValidationResultRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for payment filtering endpoints (User Story 3.7).
 * 
 * Tests:
 * - Filter by status
 * - Filter by account
 * - Filter by currency
 * - Filter by date range
 * - Filter by failed rule
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Payment Filtering Endpoint Tests (User Story 3.7)")
class PaymentFilteringTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ValidationResultRepository validationResultRepository;
    
    /**
     * Test: Filter payments by status (COMPLETED).
     * Creates payments with different statuses and verifies filtering works.
     */
    @Test
    @DisplayName("Filter payments by status")
    void testFilterByStatus() throws Exception {
        // Create test payments
        PaymentRecord payment1 = createTestPayment("100000000001", "200000000001", new BigDecimal("1000.00"), "USD", PaymentStatus.COMPLETED);
        PaymentRecord payment2 = createTestPayment("100000000002", "200000000002", new BigDecimal("2000.00"), "USD", PaymentStatus.FAILED);
        PaymentRecord payment3 = createTestPayment("100000000003", "200000000003", new BigDecimal("3000.00"), "USD", PaymentStatus.COMPLETED);
        
        // Query with status=COMPLETED filter
        mockMvc.perform(get("/api/payments")
            .param("status", "COMPLETED")
            .param("limit", "10")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$[*].status", everyItem(equalTo("COMPLETED"))));
    }
    
    /**
     * Test: Filter payments by account (source or destination).
     * Verifies that filtering matches either source or destination account.
     */
    @Test
    @DisplayName("Filter payments by account")
    void testFilterByAccount() throws Exception {
        // Create test payments
        String targetAccount = "100000000099";
        PaymentRecord payment1 = createTestPayment(targetAccount, "200000000001", new BigDecimal("1000.00"), "USD", PaymentStatus.COMPLETED);
        PaymentRecord payment2 = createTestPayment("100000000001", targetAccount, new BigDecimal("2000.00"), "USD", PaymentStatus.COMPLETED);
        PaymentRecord payment3 = createTestPayment("100000000002", "200000000002", new BigDecimal("3000.00"), "USD", PaymentStatus.COMPLETED);
        
        // Query with account filter
        mockMvc.perform(get("/api/payments")
            .param("account", targetAccount)
            .param("limit", "10")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }
    
    /**
     * Test: Filter payments by currency.
     * Creates payments in different currencies and verifies filtering works.
     */
    @Test
    @DisplayName("Filter payments by currency")
    void testFilterByCurrency() throws Exception {
        // Create test payments in different currencies
        PaymentRecord payment1 = createTestPayment("100000000001", "200000000001", new BigDecimal("1000.00"), "USD", PaymentStatus.COMPLETED);
        PaymentRecord payment2 = createTestPayment("100000000002", "200000000002", new BigDecimal("500.00"), "EUR", PaymentStatus.COMPLETED);
        PaymentRecord payment3 = createTestPayment("100000000003", "200000000003", new BigDecimal("1000.00"), "USD", PaymentStatus.COMPLETED);
        
        // Query with currency=USD filter
        mockMvc.perform(get("/api/payments")
            .param("currency", "USD")
            .param("limit", "10")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$[*].currency", everyItem(equalTo("USD"))));
    }
    
    /**
     * Test: Filter payments by date range.
     * Verifies that date-from and date-to filters work correctly.
     */
    @Test
    @DisplayName("Filter payments by date range")
    void testFilterByDateRange() throws Exception {
        // Create test payments
        PaymentRecord payment1 = createTestPayment("100000000001", "200000000001", new BigDecimal("1000.00"), "USD", PaymentStatus.COMPLETED);
        
        // Query with date range filter
        String dateFrom = "2026-07-01T00:00:00";
        String dateTo = "2026-07-31T23:59:59";
        
        mockMvc.perform(get("/api/payments")
            .param("date-from", dateFrom)
            .param("date-to", dateTo)
            .param("limit", "10")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
    
    /**
     * Test: Multiple filters combined.
     * Verifies that multiple filters work together to narrow results.
     */
    @Test
    @DisplayName("Filter with multiple criteria")
    void testFilterWithMultipleCriteria() throws Exception {
        // Create test payments
        PaymentRecord payment1 = createTestPayment("100000000001", "200000000001", new BigDecimal("1000.00"), "USD", PaymentStatus.COMPLETED);
        PaymentRecord payment2 = createTestPayment("100000000002", "200000000002", new BigDecimal("2000.00"), "EUR", PaymentStatus.COMPLETED);
        PaymentRecord payment3 = createTestPayment("100000000003", "200000000003", new BigDecimal("3000.00"), "USD", PaymentStatus.FAILED);
        
        // Query with multiple filters: status=COMPLETED AND currency=USD
        mockMvc.perform(get("/api/payments")
            .param("status", "COMPLETED")
            .param("currency", "USD")
            .param("limit", "10")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].status", everyItem(equalTo("COMPLETED"))))
            .andExpect(jsonPath("$[*].currency", everyItem(equalTo("USD"))));
    }
    
    /**
     * Test: Invalid date format should return 400 Bad Request.
     */
    @Test
    @DisplayName("Invalid date format returns 400")
    void testInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/payments")
            .param("date-from", "2026-07-01")  // Missing time component
            .param("limit", "10")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test: Pagination with limit and offset.
     * Verifies that pagination works correctly with filters.
     */
    @Test
    @DisplayName("Pagination works with filters")
    void testPaginationWithFilters() throws Exception {
        // Create multiple test payments
        for (int i = 0; i < 15; i++) {
            createTestPayment(
                String.format("10000000%04d", i),
                String.format("20000000%04d", i),
                new BigDecimal("1000.00"),
                "USD",
                PaymentStatus.COMPLETED
            );
        }
        
        // Query first page with limit=5
        mockMvc.perform(get("/api/payments")
            .param("status", "COMPLETED")
            .param("limit", "5")
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));
        
        // Query second page with limit=5, offset=5
        mockMvc.perform(get("/api/payments")
            .param("status", "COMPLETED")
            .param("limit", "5")
            .param("offset", "5")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));
    }
    
    /**
     * Test: Invalid limit returns 400 Bad Request.
     */
    @Test
    @DisplayName("Invalid limit returns 400")
    void testInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/payments")
            .param("limit", "2000")  // Exceeds max of 1000
            .param("offset", "0")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test: Invalid offset returns 400 Bad Request.
     */
    @Test
    @DisplayName("Invalid offset returns 400")
    void testInvalidOffset() throws Exception {
        mockMvc.perform(get("/api/payments")
            .param("limit", "10")
            .param("offset", "-1")  // Negative offset
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Helper method to create test payment records.
     */
    private PaymentRecord createTestPayment(
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        String currency,
        PaymentStatus status
    ) {
        PaymentRecord payment = new PaymentRecord(
            null,
            null,  // idempotency key
            sourceAccount,
            destinationAccount,
            amount,
            currency,
            status,
            null,  // error code
            null,  // error message
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        return paymentRepository.save(payment);
    }
}

