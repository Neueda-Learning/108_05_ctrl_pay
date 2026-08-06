package com.neueda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating a bulk payment from CSV upload or manual entry.
 */
public record CreateBulkPaymentRequest(
    /**
     * Source account number (12 digits) - must belong to authenticated user.
     */
    String sourceAccount,
    
    /**
     * PIN for source account authentication (exactly 4 digits).
     */
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
    String pin,
    
    /**
     * List of individual payment items in the bulk batch.
     */
    List<BulkPaymentItemDTO> items,
    
    /**
     * Optional client-provided idempotency key for duplicate prevention.
     */
    String idempotencyKey
) {}

