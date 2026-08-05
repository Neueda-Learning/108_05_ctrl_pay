package com.neueda.controller;

import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.PaymentResponse;
import com.neueda.dto.StatusTransitionRequest;
import com.neueda.exception.PaymentNotFoundException;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.service.FraudRiskService;
import com.neueda.service.PaymentService;

import jakarta.validation.Valid;

/**
 * REST Controller for Payment Lifecycle operations.
 * 
 * Endpoints:
 * - POST /api/payments/{id}/validate - Transition CREATED → VALIDATED
 * - POST /api/payments/{id}/send - Transition VALIDATED → SENT (with mock gateway simulation)
 * - POST /api/payments/{id}/complete - Transition SENT → COMPLETED (with mock confirmation)
 * - POST /api/payments/{id}/fail - Transition to FAILED (manual failure)
 */
@RestController
@RequestMapping("/api/payments/{id}")
public class PaymentLifecycleController {
    
    private final PaymentService paymentService;
    private final FraudRiskService fraudRiskService;
    private static final Random random = new Random();
    
    public PaymentLifecycleController(PaymentService paymentService, FraudRiskService fraudRiskService) {
        this.paymentService = paymentService;
        this.fraudRiskService = fraudRiskService;
    }
    
    /**
     * Validate a payment (CREATED → VALIDATED).
     * 
     * Request: POST /api/payments/{id}/validate
     * 
     * Response:
     * - 200 OK: Payment validated successfully
     * - 404 Not Found: Payment does not exist
     * - 400 Bad Request: Current status is not CREATED or validation failed
     * - 409 Conflict: Invalid state transition
     * - 500 Internal Server Error: Server error
     * 
     * @param paymentId payment ID
     * @param request optional request body (for future enhancements)
     * @return 200 OK with updated PaymentResponse
     */
    @PostMapping("/validate")
    public ResponseEntity<PaymentResponse> validatePayment(
        @PathVariable(name = "id") Long paymentId,
        @RequestBody(required = false) StatusTransitionRequest request
    ) {
        try {
            // Retrieve payment
            PaymentRecord payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            // Check current status
            if (payment.status() != PaymentStatus.CREATED) {
                throw new IllegalStateException(
                    String.format("Cannot validate payment in %s status. Must be in CREATED status.", payment.status())
                );
            }
            
            // Transition to VALIDATED
            PaymentRecord updatedPayment = paymentService.transitionPayment(paymentId, PaymentStatus.VALIDATED);
            
            PaymentResponse response = toPaymentResponse(updatedPayment);
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new PaymentProcessingException("Error validating payment: " + e.getMessage());
        }
    }
    
    /**
     * Send a payment to gateway (VALIDATED → SENT).
     * 
     * Simulates network call to payment gateway:
     * - 80% success rate → transitions to SENT
     * - 20% failure rate → transitions to FAILED with error
     * 
     * Request: POST /api/payments/{id}/send
     * 
     * Response:
     * - 200 OK: Payment sent successfully (or failed with error details)
     * - 404 Not Found: Payment does not exist
     * - 409 Conflict: Current status is not VALIDATED
     * - 500 Internal Server Error: Server error
     * 
     * @param paymentId payment ID
     * @param request optional request body
     * @return 200 OK with updated PaymentResponse
     */
    @PostMapping("/send")
    public ResponseEntity<PaymentResponse> sendPayment(
        @PathVariable(name = "id") Long paymentId,
        @RequestBody(required = false) StatusTransitionRequest request
    ) {
        try {
            // Retrieve payment
            PaymentRecord payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            // Check current status
            if (payment.status() != PaymentStatus.VALIDATED) {
                throw new IllegalStateException(
                    String.format("Cannot send payment in %s status. Must be in VALIDATED status.", payment.status())
                );
            }
            
            // Simulate network call to payment gateway
            // 80% success, 20% failure
            boolean gatewaySuccess = random.nextDouble() < 0.8;
            
            PaymentRecord updatedPayment;
            if (gatewaySuccess) {
                // Success: transition to SENT
                updatedPayment = paymentService.transitionPayment(paymentId, PaymentStatus.SENT);
            } else {
                // Simulate gateway error
                updatedPayment = paymentService.failPayment(
                    paymentId,
                    "NETWORK_ERROR",
                    "Payment gateway rejected the transaction (simulated failure)"
                );
            }
            
            PaymentResponse response = toPaymentResponse(updatedPayment);
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new PaymentProcessingException("Error sending payment: " + e.getMessage());
        }
    }
    
    /**
     * Complete a payment (SENT → COMPLETED).
     * 
     * Simulates confirmation from payment gateway:
     * - 95% success rate → transitions to COMPLETED
     * - 5% failure rate → transitions to FAILED with error
     * 
     * Request: POST /api/payments/{id}/complete
     * 
     * Response:
     * - 200 OK: Payment completed successfully (or failed with error details)
     * - 404 Not Found: Payment does not exist
     * - 409 Conflict: Current status is not SENT
     * - 500 Internal Server Error: Server error
     * 
     * @param paymentId payment ID
     * @param request optional request body
     * @return 200 OK with updated PaymentResponse
     */
    @PostMapping("/complete")
    public ResponseEntity<PaymentResponse> completePayment(
        @PathVariable(name = "id") Long paymentId,
        @RequestBody(required = false) StatusTransitionRequest request
    ) {
        try {
            // Retrieve payment
            PaymentRecord payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            // Check current status
            if (payment.status() != PaymentStatus.SENT) {
                throw new IllegalStateException(
                    String.format("Cannot complete payment in %s status. Must be in SENT status.", payment.status())
                );
            }
            
            // Simulate confirmation from gateway
            // 95% success, 5% failure
            boolean confirmationSuccess = random.nextDouble() < 0.95;
            
            PaymentRecord updatedPayment;
            if (confirmationSuccess) {
                // Success: transition to COMPLETED
                updatedPayment = paymentService.transitionPayment(paymentId, PaymentStatus.COMPLETED);
            } else {
                // Simulate gateway confirmation failure
                updatedPayment = paymentService.failPayment(
                    paymentId,
                    "PROCESSING_ERROR",
                    "Payment gateway failed to confirm the transaction (simulated failure)"
                );
            }
            
            PaymentResponse response = toPaymentResponse(updatedPayment);
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new PaymentProcessingException("Error completing payment: " + e.getMessage());
        }
    }
    
    /**
     * Manually fail a payment.
     * 
     * Allows manual failure from any non-terminal state (not COMPLETED or FAILED).
     * Useful for administrative operations or handling exceptional cases.
     * 
     * Request: POST /api/payments/{id}/fail
     * Body: StatusTransitionRequest (errorCode, errorMessage)
     * 
     * Response:
     * - 200 OK: Payment failed successfully
     * - 404 Not Found: Payment does not exist
     * - 409 Conflict: Payment is already in terminal state (COMPLETED or FAILED)
     * - 500 Internal Server Error: Server error
     * 
     * @param paymentId payment ID
     * @param request status transition request with error details
     * @return 200 OK with updated PaymentResponse
     */
    @PostMapping("/fail")
    public ResponseEntity<PaymentResponse> failPayment(
        @PathVariable(name = "id") Long paymentId,
        @RequestBody(required = false) StatusTransitionRequest request
    ) {
        try {
            // Retrieve payment
            PaymentRecord payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
            
            // Get error details from request or use defaults
            String errorCode = request != null && request.errorCode() != null 
                ? request.errorCode() 
                : "MANUAL_FAILURE";
            String errorMessage = request != null && request.errorMessage() != null 
                ? request.errorMessage() 
                : "Payment manually failed via API";
            
            // Transition to FAILED
            PaymentRecord updatedPayment = paymentService.failPayment(paymentId, errorCode, errorMessage);
            
            PaymentResponse response = toPaymentResponse(updatedPayment);
            return ResponseEntity.ok(response);
            
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new PaymentProcessingException("Error failing payment: " + e.getMessage());
        }
    }
    
    /**
     * Convert PaymentRecord to PaymentResponse.
     */
    private PaymentResponse toPaymentResponse(PaymentRecord payment) {
        FraudRiskService.PaymentRisk paymentRisk = fraudRiskService.assessPaymentRisk(payment);

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
            paymentRisk.fraudProbability(),
            paymentRisk.highRisk(),
            validationResults
        );
    }
}

