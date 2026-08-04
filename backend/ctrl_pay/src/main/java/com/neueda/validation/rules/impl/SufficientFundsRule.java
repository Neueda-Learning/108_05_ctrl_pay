package com.neueda.validation.rules.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.service.AccountService;
import com.neueda.validation.rules.ValidationRule;

/**
 * Real implementation of sufficient funds validation.
 * Checks if the source account has sufficient balance for the payment.
 * 
 * This replaces the previous mock implementation with actual account balance validation.
 * 
 * Rule Type: SUFFICIENT_FUNDS
 * Rule Definition: { "type": "SUFFICIENT_FUNDS", "message": "..." }
 * 
 * Process:
 * 1. Look up source account by account number
 * 2. Verify account exists
 * 3. Verify account currency matches payment currency
 * 4. Compare account balance against payment amount
 * 5. Return PASS if sufficient, FAIL if insufficient
 */
public class SufficientFundsRule implements ValidationRule {

    private static final Logger logger = LoggerFactory.getLogger(SufficientFundsRule.class);
    
    private final AccountService accountService;

    /**
     * Constructor: requires AccountService for account lookup.
     * This service is injected by RuleFactory.
     */
    public SufficientFundsRule(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ValidationRuleResult execute(PaymentRecord payment, JsonNode ruleDefinition) {
        long startTime = System.currentTimeMillis();

        try {
            // Extract error message with null safety
            JsonNode messageNode = ruleDefinition.get("message");
            String defaultMessage = "Source account does not have sufficient balance";
            String errorMessage = (messageNode != null) 
                ? messageNode.asText(defaultMessage)
                : defaultMessage;

            // Step 1: Look up source account by account number
            AccountRecord sourceAccount = accountService.getAccountByAccountNumber(payment.sourceAccount())
                .orElseThrow(() -> new AccountLookupException("Account not found: " + payment.sourceAccount()));

            logger.debug("Found source account {} with balance {} {}", 
                payment.sourceAccount(), 
                sourceAccount.accountBalance(), 
                sourceAccount.currency());

            // Step 2: Verify currencies match
            // NOTE: In a real system with cross-currency payments, would use CurrencyConversionService here
            // For now, we require exact currency match
            if (!sourceAccount.currency().equalsIgnoreCase(payment.currency())) {
                String mismatchError = String.format(
                    "Currency mismatch: account is %s but payment is %s",
                    sourceAccount.currency(),
                    payment.currency()
                );
                logger.warn(mismatchError);
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("CURRENCY_MISMATCH", mismatchError, executionTime);
            }

            // Step 3: Compare balance against payment amount
            if (sourceAccount.accountBalance().compareTo(payment.amount()) >= 0) {
                // Sufficient funds
                logger.debug("Sufficient funds check passed: {} >= {}", 
                    sourceAccount.accountBalance(), 
                    payment.amount());
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.success(executionTime);
            } else {
                // Insufficient funds
                String insufficientError = String.format(
                    "%s (available: %s, required: %s)",
                    errorMessage,
                    sourceAccount.accountBalance(),
                    payment.amount()
                );
                logger.warn("Insufficient funds: account balance {} < payment amount {}", 
                    sourceAccount.accountBalance(), 
                    payment.amount());
                long executionTime = System.currentTimeMillis() - startTime;
                return ValidationRuleResult.failure("INSUFFICIENT_FUNDS", insufficientError, executionTime);
            }

        } catch (AccountLookupException e) {
            // Account not found
            logger.error("Account lookup failed: {}", e.getMessage());
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure("ACCOUNT_NOT_FOUND", e.getMessage(), executionTime);

        } catch (Exception e) {
            // Unexpected error during execution
            logger.error("Unexpected error executing SUFFICIENT_FUNDS rule", e);
            long executionTime = System.currentTimeMillis() - startTime;
            return ValidationRuleResult.failure(
                "VALIDATION_ERROR",
                "Error executing SUFFICIENT_FUNDS rule: " + e.getMessage(),
                executionTime
            );
        }
    }

    /**
     * Custom exception for account lookup failures.
     */
    private static class AccountLookupException extends Exception {
        AccountLookupException(String message) {
            super(message);
        }
    }
}

