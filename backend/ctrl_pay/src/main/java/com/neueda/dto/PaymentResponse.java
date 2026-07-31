package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.neueda.domain.PaymentStatus;

/**
 * Response DTO for payment details.
 * Returned by API endpoints when retrieving a single payment.
 * Includes payment details, status, and embedded validation results for transparency.
 */
public record PaymentResponse(
    /**
     * Unique payment identifier.
     */
    Long id,
    
    /**
     * Idempotency key (null if not provided).
     */
    String idempotencyKey,
    
    /**
     * Source account number.
     */
    String sourceAccount,
    
    /**
     * Destination account number.
     */
    String destinationAccount,
    
    /**
     * Payment amount.
     */
    BigDecimal amount,
    
    /**
     * ISO 4217 currency code.
     */
    String currency,
    
    /**
     * Current payment status (CREATED, VALIDATED, SENT, COMPLETED, FAILED).
     */
    PaymentStatus status,
    
    /**
     * Error code if payment failed (null if not failed).
     */
    String errorCode,
    
    /**
     * Human-readable error message if payment failed (null if not failed).
     */
    String errorMessage,
    
    /**
     * Payment creation timestamp (ISO 8601 format).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    /**
     * Last update timestamp (ISO 8601 format).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    /**
     * Validation results - shows all validation rules executed and their outcomes.
     * Provides transparency into why a payment failed (if applicable).
     */
    List<ValidationResultResponse> validationResults
) {
}

