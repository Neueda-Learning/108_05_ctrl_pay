package com.neueda.scheduler;

import java.time.LocalDateTime;
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
import com.neueda.service.PaymentSettlementService;

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
    private final PaymentSettlementService settlementService;
    private final Random random = new Random();
    
    // Configuration (injected from application.properties)
    private final int intervalMs;
    private final double failureRate;
    
    public PaymentProcessorScheduler(
        PaymentRepository paymentRepository,
        PaymentService paymentService,
        PaymentSettlementService settlementService,
        PaymentSchedulerProperties schedulerProperties
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.settlementService = settlementService;
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
            int skippedCount = 0;
            
            for (PaymentRecord payment : validatedPayments) {
                try {
                    // DEFENSIVE CHECK: Verify payment status before processing
                    if (payment.status() != PaymentStatus.VALIDATED) {
                        skippedCount++;
                        logger.warn("Expected VALIDATED but found {} for payment id={}. Skipping payment", 
                            payment.status(), payment.id());
                        continue;
                    }
                    
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
            
            logger.info("Completed batch processing: {} successful, {} failed, {} skipped out of {} total", 
                successCount, failureCount, skippedCount, validatedPayments.size());
            
        } catch (Exception e) {
            // This catches issues fetching payments, not individual payment processing
            logger.error("Error in processValidatedPayments scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process SENT payments with settlement (debit/credit) and retry logic.
     * 
     * Settlement Lifecycle:
     * 1. Find SENT payments ready for settlement (first attempt or retry)
     * 2. Call PaymentSettlementService.settlePayment() for each
     * 3. Settlement service handles:
     *    - Atomic debit/credit transactions
     *    - Retry management with exponential backoff
     *    - Idempotency checks to prevent double-settlement
     *    - Distinguishing between retryable and non-retryable failures
     * 
     * Each settlement attempt runs in its own transaction (REQUIRES_NEW).
     * Failed payments with retries remaining are automatically scheduled for next attempt.
     * 
     * Runs every N seconds (configured via scheduler.interval-ms).
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
            
            int settlementAttempts = 0;
            int retriesScheduled = 0;
            int completedPayments = 0;
            int failedPayments = 0;
            int skippedCount = 0;
            
            for (PaymentRecord payment : sentPayments) {
                try {
                    // DEFENSIVE CHECK: Verify payment status before processing
                    if (payment.status() != PaymentStatus.SENT) {
                        skippedCount++;
                        logger.warn("Expected SENT but found {} for payment id={}. Skipping payment", 
                            payment.status(), payment.id());
                        continue;
                    }
                    
                    // Check if this payment is ready for settlement attempt
                    boolean readyForSettlement = isReadyForSettlement(payment);
                    
                    if (!readyForSettlement) {
                        logger.debug("Payment {} not ready for settlement yet. Next retry: {}",
                            payment.id(), payment.nextSettlementRetryTime());
                        skippedCount++;
                        continue;
                    }
                    
                    // Simulate network latency
                    simulateLatency();
                    
                    // Attempt settlement (service handles retries internally)
                    settlementService.settlePayment(payment.id());
                    settlementAttempts++;
                    
                    // Check if payment is now completed or still pending retry
                    PaymentRecord updated = paymentRepository.findById(payment.id())
                        .orElseThrow(() -> new RuntimeException("Payment disappeared"));
                    
                    if (updated.status() == PaymentStatus.COMPLETED) {
                        completedPayments++;
                        logger.debug("Payment {} successfully settled and completed", payment.id());
                    } else if (updated.status() == PaymentStatus.SENT && updated.nextSettlementRetryTime() != null) {
                        retriesScheduled++;
                        logger.debug("Payment {} settlement attempt {}/{} failed. Retry scheduled for {}",
                            payment.id(), 
                            updated.settlementAttemptCount(), 
                            updated.maxSettlementAttempts(),
                            updated.nextSettlementRetryTime());
                    } else if (updated.status() == PaymentStatus.FAILED) {
                        failedPayments++;
                        logger.error("Payment {} settlement failed: {} - {}",
                            payment.id(), updated.errorCode(), updated.errorMessage());
                    }
                    
                } catch (Exception e) {
                    failedPayments++;
                    logger.error("Unexpected error processing payment {}: {}", payment.id(), e.getMessage(), e);
                    // Continue processing other payments even if this one fails
                }
            }
            
            logger.info("Completed batch processing: {} settlement attempts, {} completed, {} retries scheduled, {} failed, {} skipped",
                settlementAttempts, completedPayments, retriesScheduled, failedPayments, skippedCount);
            
        } catch (Exception e) {
            // This catches issues fetching payments, not individual payment processing
            logger.error("Error in processSentPayments scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if a SENT payment is ready for settlement attempt.
     * Returns true if:
     * - Settlement has never been attempted (settlementAttemptCount == 0)
     * - OR next retry time has arrived (nextSettlementRetryTime <= now)
     */
    private boolean isReadyForSettlement(PaymentRecord payment) {
        if (payment.settlementAttemptCount() == 0) {
            return true; // First settlement attempt
        }
        
        if (payment.nextSettlementRetryTime() != null && 
            payment.nextSettlementRetryTime().isBefore(LocalDateTime.now())) {
            return true; // Retry is ready
        }
        
        return false; // Not ready yet
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





