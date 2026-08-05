package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;

/**
 * Payment Settlement Service - Handles real account debit/credit operations.
 * 
 * Responsibilities:
 * 1. Execute atomic debit/credit transactions for payments
 * 2. Implement retry logic with exponential backoff for transient failures
 * 3. Distinguish between retryable (technical) and non-retryable (business) failures
 * 4. Ensure idempotency to prevent double-settlement
 * 5. Update account balances and payment status atomically
 * 
 * Retry Strategy:
 * - Max 3 attempts for SENT → COMPLETED transition
 * - Retryable failures: Database errors, deadlocks, optimistic lock conflicts
 * - Non-retryable failures: Insufficient funds, account not found, business validation failures
 * - Uses stored retry state (not in-memory loops) for production-grade handling
 * - Each retry starts a new transaction to avoid partial completion
 */
@Service
public class PaymentSettlementService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentSettlementService.class);
    private static final int RETRY_DELAY_SECONDS = 5;
    
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final CurrencyConversionService currencyConversionService;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    
    public PaymentSettlementService(
        PaymentRepository paymentRepository,
        AccountRepository accountRepository,
        CurrencyConversionService currencyConversionService,
        PaymentStatusHistoryRepository paymentStatusHistoryRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.currencyConversionService = currencyConversionService;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
    }
    
    /**
     * Attempt to settle a payment by debiting source account and crediting destination account.
     * 
     * Process:
     * 1. Load payment and accounts
     * 2. Verify payment is not already settled (idempotency check)
     * 3. Verify sufficient funds at settlement time (double-check)
     * 4. Debit source account
     * 5. Credit destination account
     * 6. Mark payment as COMPLETED
     * 
     * If any step fails:
     * - For technical failures: Update retry state and return to scheduler
     * - For business failures: Mark payment as FAILED immediately
     * 
     * @param paymentId payment to settle
     * @throws IllegalArgumentException if payment not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settlePayment(Long paymentId) {
        try {
            logger.debug("Starting settlement attempt for payment {}", paymentId);
            
            // Step 1: Load payment record
            PaymentRecord payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> {
                    logger.error("Payment not found for settlement: {}", paymentId);
                    return new SettlementException("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId, false);
                });
            
            // Step 2: Idempotency check - if already settled, skip processing
            if (payment.status() == PaymentStatus.COMPLETED && payment.settledAt() != null) {
                logger.info("Payment {} already settled at {}. Skipping re-settlement.", paymentId, payment.settledAt());
                return;
            }
            
            // Step 3: Verify payment is in SENT status
            if (payment.status() != PaymentStatus.SENT) {
                logger.warn("Payment {} is not in SENT status (current: {}). Cannot settle.", paymentId, payment.status());
                throw new SettlementException(
                    "INVALID_PAYMENT_STATE",
                    "Payment must be in SENT status to settle (current: " + payment.status() + ")",
                    false // non-retryable
                );
            }
            
            // Step 4: Load accounts
            AccountRecord sourceAccount = accountRepository
                .findByAccountNumber(payment.sourceAccount())
                .orElseThrow(() -> {
                    logger.error("Source account not found: {}", payment.sourceAccount());
                    return new SettlementException(
                        "ACCOUNT_NOT_FOUND",
                        "Source account not found: " + payment.sourceAccount(),
                        false // non-retryable
                    );
                });
            
            AccountRecord destinationAccount = accountRepository
                .findByAccountNumber(payment.destinationAccount())
                .orElseThrow(() -> {
                    logger.error("Destination account not found: {}", payment.destinationAccount());
                    return new SettlementException(
                        "ACCOUNT_NOT_FOUND",
                        "Destination account not found: " + payment.destinationAccount(),
                        false // non-retryable
                    );
                });
            
            // Step 5: Double-check sufficient funds at settlement time
            if (sourceAccount.accountBalance().compareTo(payment.amount()) < 0) {
                logger.warn("Insufficient funds at settlement for payment {}. Balance: {} < Amount: {}",
                    paymentId, sourceAccount.accountBalance(), payment.amount());
                throw new SettlementException(
                    "INSUFFICIENT_FUNDS_AT_SETTLEMENT",
                    String.format("Insufficient funds at settlement (available: %s, required: %s)",
                        sourceAccount.accountBalance(), payment.amount()),
                    false // non-retryable
                );
            }
            
            // Step 6: Debit source account
            BigDecimal newSourceBalance = sourceAccount.accountBalance().subtract(payment.amount());
            AccountRecord updatedSourceAccount = sourceAccount.withNewBalance(newSourceBalance);
            accountRepository.update(updatedSourceAccount);
            logger.debug("Debited source account {} by {}. New balance: {}",
                sourceAccount.accountNumber(), payment.amount(), newSourceBalance);
            
            // Step 7: Perform currency conversion from source to destination currency
            CurrencyConversionResponse conversionResponse = currencyConversionService.convert(
                new CurrencyConversionRequest(
                    payment.amount(),                      // amount to convert
                    payment.currency(),                    // source currency
                    destinationAccount.currency()          // destination currency
                )
            );
            
            BigDecimal destinationAmount = conversionResponse.convertedAmount();
            BigDecimal exchangeRate = conversionResponse.exchangeRate();
            
            logger.debug("Currency conversion: {} {} x {} = {} {}",
                payment.amount(), payment.currency(), exchangeRate, destinationAmount, destinationAccount.currency());
            
            // Step 8: Credit destination account with CONVERTED amount
            BigDecimal newDestinationBalance = destinationAccount.accountBalance().add(destinationAmount);
            AccountRecord updatedDestinationAccount = destinationAccount.withNewBalance(newDestinationBalance);
            accountRepository.update(updatedDestinationAccount);
            logger.debug("Credited destination account {} by {} (converted from {}). New balance: {}",
                destinationAccount.accountNumber(), destinationAmount, payment.amount(), newDestinationBalance);
            
            // Step 9: Mark payment as COMPLETED with settlement amounts and exchange rate
            PaymentRecord settledPayment = payment.withSuccessfulSettlement(
                payment.amount(),      // sourceAmount - what was debited from source
                destinationAmount,     // destinationAmount - what was credited to destination (CONVERTED)
                exchangeRate           // exchangeRate - the rate used for conversion
            );
            paymentRepository.update(settledPayment);
            
            // Log status transition to history (SENT → COMPLETED)
            paymentStatusHistoryRepository.save(
                PaymentStatusHistoryRecord.transition(
                    paymentId,
                    PaymentStatus.SENT,
                    PaymentStatus.COMPLETED,
                    "SETTLEMENT"
                )
            );
            
            logger.info("Payment {} successfully settled. Source: {} {}, Destination: {} {}, Rate: {}. Accounts updated, payment marked COMPLETED.",
                paymentId, payment.amount(), payment.currency(), destinationAmount, destinationAccount.currency(), exchangeRate);
            
        } catch (SettlementException e) {
            handleSettlementFailure(paymentId, e);
        } catch (Exception e) {
            logger.error("Unexpected error during settlement of payment {}: {}", paymentId, e.getMessage(), e);
            handleUnexpectedFailure(paymentId, e);
        }
    }
    
    /**
     * Handle settlement failure with retry logic.
     * 
     * For retryable failures: Store retry state and schedule retry
     * For non-retryable failures: Mark payment as FAILED immediately
     */
    private void handleSettlementFailure(Long paymentId, SettlementException e) {
        PaymentRecord payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment disappeared: " + paymentId));
        
        if (e.isRetryable() && payment.settlementAttemptCount() < payment.maxSettlementAttempts()) {
            // This is a transient failure - schedule retry
            LocalDateTime nextRetryTime = LocalDateTime.now()
                .plusSeconds(RETRY_DELAY_SECONDS * (long) Math.pow(2, payment.settlementAttemptCount()));
            
            PaymentRecord retryPayment = payment.withSettlementAttempt(nextRetryTime);
            paymentRepository.update(retryPayment);
            
            logger.warn("Settlement attempt {} failed for payment {} with retryable error: {}. Scheduled next retry for {}",
                payment.settlementAttemptCount() + 1, paymentId, e.getErrorCode(), nextRetryTime);
            
        } else if (e.isRetryable()) {
            // Max retries exceeded - mark as FAILED
            PaymentRecord failedPayment = payment.withFailure(
                "SETTLEMENT_FAILED_MAX_RETRIES",
                "Settlement failed after " + payment.maxSettlementAttempts() + " attempts: " + e.getMessage()
            );
            paymentRepository.update(failedPayment);
            logger.error("Payment {} failed after {} settlement attempts", paymentId, payment.maxSettlementAttempts());
            
        } else {
            // Non-retryable business failure - fail immediately
            PaymentRecord failedPayment = payment.withFailure(e.getErrorCode(), e.getMessage());
            paymentRepository.update(failedPayment);
            logger.error("Payment {} failed with non-retryable error {}: {}", paymentId, e.getErrorCode(), e.getMessage());
        }
    }
    
    /**
     * Handle unexpected technical errors (database, network, etc).
     * Treat as retryable.
     */
    private void handleUnexpectedFailure(Long paymentId, Exception e) {
        PaymentRecord payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment disappeared: " + paymentId));
        
        if (payment.settlementAttemptCount() < payment.maxSettlementAttempts()) {
            LocalDateTime nextRetryTime = LocalDateTime.now()
                .plusSeconds(RETRY_DELAY_SECONDS * (long) Math.pow(2, payment.settlementAttemptCount()));
            
            PaymentRecord retryPayment = payment.withSettlementAttempt(nextRetryTime);
            paymentRepository.update(retryPayment);
            
            logger.warn("Settlement for payment {} scheduled for retry due to technical error: {}", paymentId, e.getMessage());
            
        } else {
            PaymentRecord failedPayment = payment.withFailure(
                "SETTLEMENT_FAILED_TECHNICAL",
                "Settlement failed after " + payment.maxSettlementAttempts() + " attempts due to: " + e.getMessage()
            );
            paymentRepository.update(failedPayment);
            logger.error("Payment {} failed after {} attempts due to technical error", paymentId, payment.maxSettlementAttempts());
        }
    }
    
    /**
     * Settlement exception - distinguishes between retryable and non-retryable failures.
     */
    public static class SettlementException extends RuntimeException {
        private final String errorCode;
        private final boolean retryable;
        
        public SettlementException(String errorCode, String message, boolean retryable) {
            super(message);
            this.errorCode = errorCode;
            this.retryable = retryable;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
}


