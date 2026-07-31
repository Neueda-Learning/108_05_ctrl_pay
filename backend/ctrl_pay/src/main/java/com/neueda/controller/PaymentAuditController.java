package com.neueda.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.dto.StatusHistoryItemResponse;
import com.neueda.dto.ValidationResultResponse;
import com.neueda.exception.PaymentNotFoundException;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.service.PaymentService;

/**
 * REST Controller for Payment Audit Trail operations.
 * 
 * Endpoints:
 * - GET /api/payments/{id}/audit/status-history - Get payment status transition history
 * - GET /api/payments/{id}/audit/validations - Get payment validation results
 */
@RestController
@RequestMapping("/api/payments/{id}/audit")
public class PaymentAuditController {
    
    private final PaymentService paymentService;
    
    public PaymentAuditController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Get payment status history (audit trail of all transitions).
     * 
     * Request: GET /api/payments/{id}/audit/status-history
     * 
     * Response:
     * - 200 OK: List of status transitions in chronological order
     * - 404 Not Found: Payment does not exist
     * - 500 Internal Server Error: Server error
     * 
     * @param paymentId payment ID
     * @return 200 OK with list of status history items
     */
    @GetMapping("/status-history")
    public ResponseEntity<List<StatusHistoryItemResponse>> getStatusHistory(
        @PathVariable(name = "id") Long paymentId
    ) {
        try {
            // Verify payment exists
            paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            // Retrieve status history
            List<PaymentStatusHistoryRecord> history = paymentService.getPaymentHistory(paymentId);
            
            // Convert to response DTOs
            List<StatusHistoryItemResponse> responses = history.stream()
                .map(h -> new StatusHistoryItemResponse(
                    h.oldStatus() != null ? h.oldStatus().name() : null,
                    h.newStatus().name(),
                    h.triggeredBy(),
                    h.errorCode(),
                    h.errorMessage(),
                    h.createdAt()
                ))
                .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving payment status history: " + e.getMessage());
        }
    }
    
    /**
     * Get payment validation results (audit trail of validation checks).
     * 
     * Request: GET /api/payments/{id}/audit/validations
     * 
     * Response:
     * - 200 OK: List of validation results in chronological order
     * - 404 Not Found: Payment does not exist
     * - 500 Internal Server Error: Server error
     * 
     * Shows which validation rules were executed and which ones failed.
     * Includes rule definitions at time of execution for compliance.
     * 
     * @param paymentId payment ID
     * @return 200 OK with list of validation results
     */
    @GetMapping("/validations")
    public ResponseEntity<List<ValidationResultResponse>> getValidationResults(
        @PathVariable(name = "id") Long paymentId
    ) {
        try {
            // Verify payment exists
            paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            // Retrieve validation results
            var results = paymentService.getValidationResults(paymentId).stream()
                .map(vr -> new ValidationResultResponse(
                    vr.validationRuleId(),
                    vr.ruleName(),
                    vr.passed(),
                    vr.errorCode(),
                    vr.errorMessage(),
                    vr.executionTimeMs()
                ))
                .toList();
            
            return ResponseEntity.ok(results);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving payment validation results: " + e.getMessage());
        }
    }
}

