package com.neueda.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * Payment Retry Service - Handles automatic retry logic for failed payments.
 * 
 * Features:
 * - Exponential backoff for retries (1s, 2s, 4s, ...)
 * - Configurable max retry attempts
 * - Automatic transition back to VALIDATED for retry
 * - Manual retry endpoint support
 */
@Service
@Transactional
public class PaymentRetryService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RetryConfiguration retryConfig;
    
    public PaymentRetryService(
        PaymentRepository paymentRepository,
        PaymentService paymentService,
        RetryConfiguration retryConfig
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.retryConfig = retryConfig;
    }
    
/**
 * Manually retry a failed payment.
 * Transitions FAILED → VALIDATED for reprocessing.
 * 
 * @param paymentId ID of payment to retry
 * @return Updated payment record
 */
public PaymentRecord retryFailedPayment(Long paymentId) {
    PaymentRecord payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    
    // Only FAILED payments can be retried
    if (payment.status() != PaymentStatus.FAILED) {
        throw new IllegalStateException(
            String.format("Cannot retry payment in %s status", payment.status())
        );
    }
    
    // Transition back to VALIDATED for reprocessing
    PaymentRecord updatedPayment = new PaymentRecord(
        paymentId,
        payment.idempotencyKey(),
        payment.sourceAccount(),
        payment.destinationAccount(),
        payment.amount(),
        payment.currency(),
        PaymentStatus.VALIDATED,  // Reset to VALIDATED
        null,  // Clear error code
        null,  // Clear error message
        payment.createdAt(),
        LocalDateTime.now()
    );
    
        PaymentRecord savedPayment = paymentRepository.update(updatedPayment);
        
        return savedPayment;
}
    
    /**
     * Calculate retry delay using exponential backoff.
     * Example: attempt 1 = 1000ms, attempt 2 = 2000ms, attempt 3 = 4000ms
     * 
     * @param retryAttempt Retry attempt number (0-indexed)
     * @return Delay in milliseconds
     */
    public long calculateBackoffDelay(int retryAttempt) {
        long initialDelay = retryConfig.getInitialDelayMs();
        return initialDelay * (long) Math.pow(2, retryAttempt);
    }
    
    /**
     * Check if a payment should be retried based on retry count and max attempts.
     * 
     * @param retryCount Current retry count
     * @return true if payment can be retried, false if max attempts exceeded
     */
    public boolean canRetry(int retryCount) {
        return retryCount < retryConfig.getMaxAttempts();
    }
}





