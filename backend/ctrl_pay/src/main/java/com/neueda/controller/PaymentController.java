package com.neueda.controller;

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
import com.neueda.exception.PaymentNotFoundException;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.exception.PaymentValidationException;
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
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Create a new payment.
     * 
     * Request: POST /api/payments
     * Body: CreatePaymentRequest (sourceAccount, destinationAccount, amount, currency, idempotencyKey)
     * 
     * Response:
     * - 201 Created: Payment created successfully
     * - 400 Bad Request: Validation error (invalid fields)
     * - 409 Conflict: Duplicate idempotency key with different details
     * - 500 Internal Server Error: Server error
     * 
     * @param request payment creation request
     * @return 201 Created with payment details
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            // Convert DTO to domain model
            PaymentRecord newPayment = PaymentRecord.create(
                request.idempotencyKey(),
                request.sourceAccount(),
                request.destinationAccount(),
                request.amount(),
                request.currency()
            );
            
            // Create payment (executes validation, logs audit trail, handles idempotency)
            PaymentRecord savedPayment = paymentService.createPayment(newPayment);
            
            // Convert domain model to response DTO
            PaymentResponse response = toPaymentResponse(savedPayment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
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
     * Request: GET /api/payments?status=COMPLETED&limit=10&offset=0
     * 
     * Query Parameters:
     * - status (optional): Filter by payment status (CREATED, VALIDATED, SENT, COMPLETED, FAILED)
     * - limit (optional, default 10): Max results to return
     * - offset (optional, default 0): Pagination offset
     * 
     * Response:
     * - 200 OK: List of payments
     * - 400 Bad Request: Invalid query parameters
     * - 500 Internal Server Error: Server error
     * 
     * @param status optional status filter
     * @param limit max results (default 10)
     * @param offset pagination offset (default 0)
     * @return 200 OK with list of payments
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> listPayments(
        @RequestParam(required = false) PaymentStatus status,
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
            
            // Query payments
            List<PaymentRecord> payments = paymentService.listPayments(status, limit, offset);
            
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
     * Includes embedded validation results for transparency.
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
            payment.createdAt(),
            payment.updatedAt(),
            validationResults
        );
    }
}


