package com.neueda.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for validation rule details.
 */
public record ValidationRuleResponse(
    /**
     * Unique rule ID.
     */
    Long id,
    
    /**
     * Rule name.
     */
    String name,
    
    /**
     * Rule description.
     */
    String description,
    
    /**
     * Rule type (AMOUNT_RANGE, CURRENCY_WHITELIST, etc.).
     */
    String ruleType,
    
    /**
     * JSON rule definition.
     */
    JsonNode ruleDefinition,
    
    /**
     * Whether rule is currently active.
     */
    boolean isActive,
    
    /**
     * Rule severity (HARD or SOFT).
     */
    String severity,
    
    /**
     * Execution order.
     */
    int orderOfExecution,
    
    /**
     * Rule creation timestamp.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    /**
     * Rule last update timestamp.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
}

