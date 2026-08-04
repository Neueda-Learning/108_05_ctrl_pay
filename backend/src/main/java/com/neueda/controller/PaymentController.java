package com.neueda.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.CreatePaymentRequest;
import com.neueda.dto.ErrorResponse;
import com.neueda.dto.PaymentResponse;
import com.neueda.exception.AccountValidationException;
import com.neueda.exception.PaymentNotFoundException;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.exception.PaymentValidationException;
import com.neueda.service.AccountService;
import com.neueda.service.PaymentService;

import jakarta.validation.Valid;

/**
 * REST Controller for Payment operations.
 * 
 * Endpoints:
 * - POST /api/payments - Create new payment
 * - GET /api/payments/{id} - Retrieve payment
 * - GET /api/payments - List payments (with filtering & pagination)
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final AccountService accountService;
    
    public PaymentController(PaymentService paymentService, AccountService accountService) {
        this.paymentService = paymentService;
        this.accountService = accountService;
    }
    
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            // Step 1: Verify PIN for source account
            try {
                accountService.verifyAccountPinByAccountNumber(request.sourceAccount(), request.pin());
            } catch (Exception e) {
                throw new AccountValidationException("PIN verification failed: " + e.getMessage(), 
                    "INVALID_PIN");
            }
            
            // Step 2: Convert DTO to domain model
            // Check if exchange rate information is provided
            PaymentRecord newPayment;
            if (request.exchangeRate() != null && request.sourceAmount() != null && request.destinationAmount() != null) {
                // Create payment with exchange rate information
                newPayment = PaymentRecord.createWithExchangeRate(
                    request.idempotencyKey(),
                    request.sourceAccount(),
                    request.destinationAccount(),
                    request.amount(),
                    request.currency(),
                    request.sourceAmount(),
                    request.destinationAmount(),
                    request.exchangeRate()
                );
            } else {
                // Create payment without exchange rate (backward compatibility)
                newPayment = PaymentRecord.create(
                    request.idempotencyKey(),
                    request.sourceAccount(),
                    request.destinationAccount(),
                    request.amount(),
                    request.currency()
                );
            }
            
            // Step 3: Create payment (executes validation, logs audit trail, handles idempotency)
            PaymentRecord savedPayment = paymentService.createPayment(newPayment);
            
            // Step 4: Convert domain model to response DTO
            PaymentResponse response = toPaymentResponse(savedPayment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (AccountValidationException e) {
            // 401 Unauthorized: PIN verification failed
            throw e;
        } catch (IllegalArgumentException e) {
            // 400 Bad Request: Invalid input
            throw new PaymentValidationException("Invalid payment details: " + e.getMessage(), 
                "VALIDATION_FAILED");
        } catch (Exception e) {
            // 500 Internal Server Error
            throw new PaymentProcessingException("Error creating payment: " + e.getMessage());
        }
    }
    
    /**
     * Retrieve a payment by ID.
     * 
     * Request: GET /api/payments/{id}
     * 
     * Response:
     * - 200 OK: Payment found
     * - 404 Not Found: Payment does not exist
     * - 500 Internal Server Error: Server error
     * 
     * @param paymentId payment ID
     * @return 200 OK with payment details, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable(name = "id") Long paymentId) {
        try {
            PaymentRecord payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            PaymentResponse response = toPaymentResponse(payment);
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving payment: " + e.getMessage());
        }
    }
    
    /**
     * List payments with optional filtering and pagination.
     * 
     * Request: GET /api/payments?status=COMPLETED&account=123456789012&currency=USD&date-from=2026-07-01T00:00:00&date-to=2026-07-31T23:59:59&failed-rule=2&limit=10&offset=0
     * 
     * Query Parameters:
     * - status (optional): Filter by payment status (CREATED, VALIDATED, SENT, COMPLETED, FAILED)
     * - account (optional): Filter by source or destination account (12-digit format)
     * - currency (optional): Filter by currency (e.g., USD, EUR)
     * - date-from (optional): Filter by created_at >= this timestamp (ISO 8601 format)
     * - date-to (optional): Filter by created_at <= this timestamp (ISO 8601 format)
     * - failed-rule (optional): Filter by failed validation rule ID (returns payments that failed this specific rule)
     * - limit (optional, default 10): Max results to return (1-1000)
     * - offset (optional, default 0): Pagination offset
     * 
     * Response:
     * - 200 OK: List of payments matching all filters
     * - 400 Bad Request: Invalid query parameters
     * - 500 Internal Server Error: Server error
     * 
     * @param status optional status filter
     * @param account optional account filter
     * @param currency optional currency filter
     * @param dateFrom optional date from filter (ISO 8601 string)
     * @param dateTo optional date to filter (ISO 8601 string)
     * @param failedRule optional failed rule ID filter
     * @param limit max results (default 10)
     * @param offset pagination offset (default 0)
     * @return 200 OK with list of payments
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> listPayments(
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(required = false) String account,
        @RequestParam(required = false) String currency,
        @RequestParam(name = "date-from", required = false) String dateFrom,
        @RequestParam(name = "date-to", required = false) String dateTo,
        @RequestParam(name = "failed-rule", required = false) Long failedRule,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        try {
            // Validate pagination parameters
            if (limit <= 0 || limit > 1000) {
                throw new IllegalArgumentException("Limit must be between 1 and 1000");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("Offset must be >= 0");
            }
            
            // Parse date parameters if provided
            LocalDateTime dateFromParsed = null;
            LocalDateTime dateToParsed = null;
            
            if (dateFrom != null && !dateFrom.isEmpty()) {
                try {
                    dateFromParsed = LocalDateTime.parse(dateFrom, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid date-from format. Use ISO 8601 (e.g., 2026-07-01T00:00:00)");
                }
            }
            
            if (dateTo != null && !dateTo.isEmpty()) {
                try {
                    dateToParsed = LocalDateTime.parse(dateTo, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid date-to format. Use ISO 8601 (e.g., 2026-07-31T23:59:59)");
                }
            }
            
            // Query payments with filters
            List<PaymentRecord> payments = paymentService.listPaymentsFiltered(
                status,
                account,
                currency,
                dateFromParsed,
                dateToParsed,
                failedRule,
                limit,
                offset
            );
            
            // Convert to response DTOs
            List<PaymentResponse> responses = payments.stream()
                .map(this::toPaymentResponse)
                .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (IllegalArgumentException e) {
            throw new PaymentValidationException("Invalid query parameters: " + e.getMessage(), 
                "INVALID_QUERY_PARAMS");
        } catch (Exception e) {
            throw new PaymentProcessingException("Error listing payments: " + e.getMessage());
        }
    }
    
    /**
     * Convert PaymentRecord (domain model) to PaymentResponse (DTO).
     * Includes embedded validation results and exchange rate information for transparency.
     * 
     * @param payment domain model
     * @return response DTO
     */
    private PaymentResponse toPaymentResponse(PaymentRecord payment) {
        // Fetch validation results from service
        var validationResults = paymentService.getValidationResults(payment.id()).stream()
            .map(vr -> new com.neueda.dto.ValidationResultResponse(
                vr.validationRuleId(),
                vr.ruleName(),
                vr.passed(),
                vr.errorCode(),
                vr.errorMessage(),
                vr.executionTimeMs()
            ))
            .toList();
        
        return new PaymentResponse(
            payment.id(),
            payment.idempotencyKey(),
            payment.sourceAccount(),
            payment.destinationAccount(),
            payment.amount(),
            payment.currency(),
            payment.status(),
            payment.errorCode(),
            payment.errorMessage(),
            payment.sourceAmount(),
            payment.destinationAmount(),
            payment.exchangeRate(),
            payment.createdAt(),
            payment.updatedAt(),
            validationResults
        );
    }
}


