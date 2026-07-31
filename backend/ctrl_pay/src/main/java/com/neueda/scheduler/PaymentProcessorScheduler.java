package com.neueda.scheduler;

import java.util.List;
import java.util.Random;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;
import com.neueda.service.PaymentService;

/**
 * Async Payment Processor - Automatically progresses payments through lifecycle stages.
 * 
 * This scheduler simulates an external payment processing system by:
 * 1. Transitioning VALIDATED payments to SENT
 * 2. Transitioning SENT payments to COMPLETED (or randomly FAILED)
 * 
 * This demonstrates async processing without requiring manual API calls.
 * Can be disabled via 'scheduler.enabled=false' configuration.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentProcessorScheduler {
    
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final Random random = new Random();
    
    // Configuration (injected from application.properties)
    private final int intervalMs;
    private final double failureRate;
    
    public PaymentProcessorScheduler(
        PaymentRepository paymentRepository,
        PaymentService paymentService,
        PaymentSchedulerProperties schedulerProperties
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.intervalMs = schedulerProperties.getIntervalMs();
        this.failureRate = schedulerProperties.getFailureRate();
    }
    
    /**
     * Transition VALIDATED payments to SENT.
     * Runs every N seconds (configured via scheduler.interval-ms).
     */
    @Scheduled(fixedRateString = "${scheduler.interval-ms:5000}")
    @Transactional
    public void processValidatedPayments() {
        try {
            List<PaymentRecord> validatedPayments = paymentRepository.findAll(PaymentStatus.VALIDATED, 100, 0);
            
            if (validatedPayments.isEmpty()) {
                return;
            }
            
            for (PaymentRecord payment : validatedPayments) {
                try {
                    // Simulate network latency
                    simulateLatency();
                    
                    // Transition to SENT
                    paymentService.transitionPayment(payment.id(), PaymentStatus.SENT);
                    
                } catch (Exception e) {
                    // Silently continue on error
                }
            }
            
        } catch (Exception e) {
            // Silently handle scheduler errors
        }
    }
    
    /**
     * Transition SENT payments to COMPLETED (or randomly FAILED).
     * Simulates the final payment processing stage.
     */
    @Scheduled(fixedRateString = "${scheduler.interval-ms:5000}", initialDelayString = "${scheduler.initial-delay-ms:2000}")
    @Transactional
    public void processSentPayments() {
        try {
            List<PaymentRecord> sentPayments = paymentRepository.findAll(PaymentStatus.SENT, 100, 0);
            
            if (sentPayments.isEmpty()) {
                return;
            }
            
            for (PaymentRecord payment : sentPayments) {
                try {
                    // Simulate network latency
                    simulateLatency();
                    
                    // Randomly fail based on configured failure rate
                    if (shouldFail()) {
                        paymentService.failPayment(
                            payment.id(),
                            "PAYMENT_FAILED",
                            "Payment failed during processing"
                        );
                    } else {
                        // Transition to COMPLETED
                        paymentService.transitionPayment(payment.id(), PaymentStatus.COMPLETED);
                    }
                    
                } catch (Exception e) {
                    // Silently continue on error
                }
            }
            
        } catch (Exception e) {
           // Silently handle scheduler errors
        }
    }
    
    /**
     * Simulate network latency (random delay).
     * Adds realism to async processing.
     */
    private void simulateLatency() {
        try {
            int latencyMs = random.nextInt(100) + 50;  // 50-150ms
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Determine if payment should fail randomly.
     * Based on configured failureRate (e.g., 0.1 = 10% failure).
     */
    private boolean shouldFail() {
        return random.nextDouble() < failureRate;
    }
}





