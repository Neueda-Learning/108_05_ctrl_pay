package com.neueda.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized error response DTO for all API error responses.
 * Used by global exception handler to return consistent error information to clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    /**
     * Machine-readable error code for programmatic handling.
     * Corresponds to ErrorCode enum values.
     */
    String errorCode,
    
    /**
     * Human-readable error message.
     */
    String message,
    
    /**
     * HTTP status code that was returned.
     */
    int status,
    
    /**
     * API endpoint path that was called when error occurred.
     */
    String path,
    
    /**
     * Timestamp when error occurred (ISO 8601 format).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    
    /**
     * Additional details for debugging (usually null in production, populated in development).
     */
    String details
) {
    
    /**
     * Convenience constructor with minimal required fields.
     * 
     * @param errorCode error code
     * @param message error message
     * @param status HTTP status code
     * @param path API path
     * @return new ErrorResponse
     */
    public static ErrorResponse of(String errorCode, String message, int status, String path) {
        return new ErrorResponse(errorCode, message, status, path, LocalDateTime.now(), null);
    }
    
    /**
     * Convenience constructor with details.
     * 
     * @param errorCode error code
     * @param message error message
     * @param status HTTP status code
     * @param path API path
     * @param details additional debugging details
     * @return new ErrorResponse
     */
    public static ErrorResponse of(String errorCode, String message, int status, String path, String details) {
        return new ErrorResponse(errorCode, message, status, path, LocalDateTime.now(), details);
    }
}

