package com.neueda.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for a single payment status history entry.
 */
public record StatusHistoryItemResponse(
    /**
     * Previous status (null for initial CREATED entry).
     */
    String oldStatus,
    
    /**
     * New status after transition.
     */
    String newStatus,
    
    /**
     * Who triggered the transition (SYSTEM, USER, RETRY, API, etc.).
     */
    String triggeredBy,
    
    /**
     * Error code if transition to FAILED (null otherwise).
     */
    String errorCode,
    
    /**
     * Error message if transition to FAILED (null otherwise).
     */
    String errorMessage,
    
    /**
     * Timestamp when transition occurred.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {
}

