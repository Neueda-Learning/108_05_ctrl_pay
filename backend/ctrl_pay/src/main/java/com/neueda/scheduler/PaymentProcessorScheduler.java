package com.neueda.scheduler;

import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorScheduler.class);
    
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
     * 
     * IMPORTANT: NOT @Transactional here. Each payment is processed in its own transaction
     * via paymentService.processValidatedPaymentToSent() which uses @Transactional(propagation = REQUIRES_NEW).
     * This ensures that if one payment fails, other successfully processed payments are still committed.
     * 
     * Runs every N seconds (configured via scheduler.interval-ms).
     */
    @Scheduled(fixedRateString = "${scheduler.interval-ms:5000}")
    public void processValidatedPayments() {
        try {
            logger.debug("Starting batch processing of VALIDATED payments");
            List<PaymentRecord> validatedPayments = paymentRepository.findAll(PaymentStatus.VALIDATED, 100, 0);
            
            if (validatedPayments.isEmpty()) {
                logger.debug("No VALIDATED payments found to process");
                return;
            }
            
            logger.info("Found {} VALIDATED payments to process", validatedPayments.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (PaymentRecord payment : validatedPayments) {
                try {
                    // Simulate network latency
                    simulateLatency();
                    
                    // Process each payment in its own transaction
                    // If this payment fails, it won't affect other payments
                    paymentService.processValidatedPaymentToSent(payment.id());
                    
                    logger.debug("Successfully transitioned payment {} to SENT", payment.id());
                    successCount++;
                    
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Failed to process payment {}: {}", payment.id(), e.getMessage(), e);
                    // Continue processing other payments even if this one fails
                }
            }
            
            logger.info("Completed batch processing: {} successful, {} failed out of {} total", 
                successCount, failureCount, validatedPayments.size());
            
        } catch (Exception e) {
            // This catches issues fetching payments, not individual payment processing
            logger.error("Error in processValidatedPayments scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Transition SENT payments to COMPLETED (or randomly FAILED).
     * Simulates the final payment processing stage.
     * 
     * IMPORTANT: NOT @Transactional here. Each payment is processed in its own transaction
     * via paymentService.processSentPaymentToCompletion() which uses @Transactional(propagation = REQUIRES_NEW).
     * This ensures independent transaction handling for each payment.
     */
    @Scheduled(fixedRateString = "${scheduler.interval-ms:5000}", initialDelayString = "${scheduler.initial-delay-ms:2000}")
    public void processSentPayments() {
        try {
            logger.debug("Starting batch processing of SENT payments");
            List<PaymentRecord> sentPayments = paymentRepository.findAll(PaymentStatus.SENT, 100, 0);
            
            if (sentPayments.isEmpty()) {
                logger.debug("No SENT payments found to process");
                return;
            }
            
            logger.info("Found {} SENT payments to process", sentPayments.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (PaymentRecord payment : sentPayments) {
                try {
                    // Simulate network latency
                    simulateLatency();
                    
                    // Randomly fail based on configured failure rate
                    if (shouldFail()) {
                        paymentService.processSentPaymentFailure(payment.id(), 
                            "PAYMENT_FAILED", 
                            "Payment failed during processing");
                        logger.debug("Payment {} marked as FAILED ", payment.id());
                    } else {
                        paymentService.processSentPaymentToCompletion(payment.id());
                        logger.debug("Successfully transitioned payment {} to COMPLETED", payment.id());
                    }
                    
                    successCount++;
                    
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Failed to process payment {}: {}", payment.id(), e.getMessage(), e);
                    // Continue processing other payments even if this one fails
                }
            }
            
            logger.info("Completed batch processing: {} successful, {} failed out of {} total", 
                successCount, failureCount, sentPayments.size());
            
        } catch (Exception e) {
            // This catches issues fetching payments, not individual payment processing
            logger.error("Error in processSentPayments scheduler: {}", e.getMessage(), e);
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





