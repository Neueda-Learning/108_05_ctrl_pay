package com.neueda.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request DTO for payment status transitions and manual failure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatusTransitionRequest(
    /**
     * Optional error code (used when manually failing a payment).
     */
    String errorCode,
    
    /**
     * Optional error message (used when manually failing a payment).
     */
    String errorMessage
) {
}

