package com.neueda.exception;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.neueda.dto.ErrorResponse;

/**
 * Global exception handler for REST API.
 * Converts all exceptions to standardized ErrorResponse format with user-friendly messages.
 * 
 * Key Features:
 * - Masks technical/database errors behind user-friendly messages
 * - Logs full exception details for debugging/support
 * - Returns appropriate HTTP status codes
 * - Consistent error response format across all endpoints
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle PaymentValidationException (400 Bad Request).
     */
    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentValidationException(
        PaymentValidationException ex,
        WebRequest request
    ) {
        logger.warn("Payment validation error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle PaymentNotFoundException (404 Not Found).
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(
        PaymentNotFoundException ex,
        WebRequest request
    ) {
        logger.info("Payment not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "PAYMENT_NOT_FOUND",
            "The payment you are looking for does not exist.",
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle CustomerValidationException (400 Bad Request).
     */
    @ExceptionHandler(CustomerValidationException.class)
    public ResponseEntity<ErrorResponse> handleCustomerValidationException(
        CustomerValidationException ex,
        WebRequest request
    ) {
        logger.warn("Customer validation error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle CustomerNotFoundException (404 Not Found).
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundException(
        CustomerNotFoundException ex,
        WebRequest request
    ) {
        logger.info("Customer not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "CUSTOMER_NOT_FOUND",
            "The customer you are looking for does not exist.",
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle AccountValidationException (400 Bad Request).
     */
    @ExceptionHandler(AccountValidationException.class)
    public ResponseEntity<ErrorResponse> handleAccountValidationException(
        AccountValidationException ex,
        WebRequest request
    ) {
        logger.warn("Account validation error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle AccountNotFoundException (404 Not Found).
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(
        AccountNotFoundException ex,
        WebRequest request
    ) {
        logger.info("Account not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "ACCOUNT_NOT_FOUND",
            "The account you are looking for does not exist.",
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle receipt access denials (403 Forbidden).
     */
    @ExceptionHandler(ReceiptAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleReceiptAccessDeniedException(
        ReceiptAccessDeniedException ex,
        WebRequest request
    ) {
        logger.warn("Receipt access denied: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "RECEIPT_ACCESS_DENIED",
            "You do not have permission to access this receipt.",
            HttpStatus.FORBIDDEN.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * Handle PaymentProcessingException (500 Internal Server Error).
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessingException(
        PaymentProcessingException ex,
        WebRequest request
    ) {
        logger.error("Payment processing error: {}", ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.of(
            "PAYMENT_PROCESSING_ERROR",
            "An error occurred while processing your payment. Please try again later or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handle ServiceException (500 Internal Server Error).
     * Generic exception for service-level errors in customer profile, accounts, and other non-payment operations.
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(
        ServiceException ex,
        WebRequest request
    ) {
        logger.error("Service error [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.of(
            ex.getErrorCode(),
            "An error occurred while processing your request. Please try again later or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handle Bulk Payment CSV validation exceptions (400 Bad Request).
     */
    @ExceptionHandler(BulkPaymentCSVValidationException.class)
    public ResponseEntity<ErrorResponse> handleBulkPaymentCSVValidationException(
        BulkPaymentCSVValidationException ex,
        WebRequest request
    ) {
        logger.warn("Bulk payment CSV validation error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "CSV_VALIDATION_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle Bulk Payment batch not found exceptions (404 Not Found).
     */
    @ExceptionHandler(BulkPaymentBatchNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBulkPaymentBatchNotFoundException(
        BulkPaymentBatchNotFoundException ex,
        WebRequest request
    ) {
        logger.info("Bulk payment batch not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "BATCH_NOT_FOUND",
            "The requested bulk payment batch does not exist.",
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle other Bulk Payment exceptions (400 Bad Request).
     */
    @ExceptionHandler(BulkPaymentException.class)
    public ResponseEntity<ErrorResponse> handleBulkPaymentException(
        BulkPaymentException ex,
        WebRequest request
    ) {
        logger.warn("Bulk payment error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle SQL exceptions from database operations (500 Internal Server Error).
     * Masks technical database errors behind user-friendly message to prevent information leakage.
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(
        SQLException ex,
        WebRequest request
    ) {
        logger.error("Database error occurred: {}", ex.getMessage(), ex);
        
        // Determine user-friendly message based on error type
        String userMessage = "A database error occurred. Please try again later.";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Unknown database")) {
                userMessage = "System configuration error. Please contact support.";
            } else if (ex.getMessage().contains("Unknown column")) {
                userMessage = "System data validation error. Please contact support.";
            } else if (ex.getMessage().contains("FOREIGN KEY")) {
                userMessage = "Cannot complete operation: referenced data does not exist.";
            } else if (ex.getMessage().contains("Duplicate entry")) {
                userMessage = "This record already exists. Please use a different value.";
            }
        }
        
        ErrorResponse response = ErrorResponse.of(
            "DATABASE_ERROR",
            userMessage,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle Spring Data Access exceptions (500 Internal Server Error).
     * Masks technical database errors behind user-friendly message.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
        DataAccessException ex,
        WebRequest request
    ) {
        logger.error("Data access error occurred: {}", ex.getMessage(), ex);
        
        String userMessage = "A database operation failed. Please try again later.";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Connection")) {
                userMessage = "Unable to connect to database. Please try again later.";
            } else if (ex.getMessage().contains("Timeout")) {
                userMessage = "Database operation timed out. Please try again.";
            }
        }
        
        ErrorResponse response = ErrorResponse.of(
            "DATABASE_ERROR",
            userMessage,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle IllegalArgumentException (400 Bad Request).
     * Common in validation and business logic checks.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        WebRequest request
    ) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "INVALID_REQUEST",
            ex.getMessage() != null ? ex.getMessage() : "Invalid request parameters provided.",
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle IllegalStateException (409 Conflict).
     * Occurs when operation is invalid for current state.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
        IllegalStateException ex,
        WebRequest request
    ) {
        logger.warn("Invalid state: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "INVALID_STATE",
            ex.getMessage() != null ? ex.getMessage() : "Operation cannot be performed in the current state.",
            HttpStatus.CONFLICT.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle NumberFormatException (400 Bad Request).
     * Occurs when numeric parameter is invalid.
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormatException(
        NumberFormatException ex,
        WebRequest request
    ) {
        logger.warn("Number format error: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            "INVALID_NUMBER_FORMAT",
            "One or more numeric values are invalid. Please check your input.",
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle validation errors from @Valid annotation (400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Request validation failed. Please check your input.");
        
        logger.warn("Validation error: {}", message);
        ErrorResponse response = ErrorResponse.of(
            "VALIDATION_FAILED",
            message,
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle all other unexpected exceptions (500 Internal Server Error).
     * This is the catch-all handler for any unhandled exceptions.
     * Does NOT expose raw technical details to user.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
        Exception ex,
        WebRequest request
    ) {
        // Log full details for debugging
        logger.error("Unexpected error occurred: " + ex.getClass().getName(), ex);
        
        // Return generic message to user without technical details
        ErrorResponse response = ErrorResponse.of(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later or contact support if the problem persists.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

