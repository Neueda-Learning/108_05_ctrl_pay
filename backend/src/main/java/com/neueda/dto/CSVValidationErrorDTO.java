package com.neueda.dto;

import java.util.List;

/**
 * DTO for a single CSV validation error.
 */
public record CSVValidationErrorDTO(
    /**
     * Row number in CSV (1-indexed).
     */
    Integer rowNumber,
    
    /**
     * Field name where error occurred (null if row-level error).
     */
    String fieldName,
    
    /**
     * Error message describing the validation failure.
     */
    String errorMessage,
    
    /**
     * Error code for programmatic handling.
     */
    String errorCode
) {}

