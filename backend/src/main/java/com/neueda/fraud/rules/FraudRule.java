package com.neueda.fraud.rules;

import java.math.BigDecimal;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Interface for extensible fraud detection rules.
 * Each rule is independent and can be added/removed without modifying existing rules.
 */
public interface FraudRule {
    
    /**
     * Unique rule identifier (enum-like string)
     */
    String getRuleName();
    
    /**
     * Human-readable rule description
     */
    String getDescription();
    
    /**
     * Evaluate this rule against a payment and source account data
     * 
     * @param payment the payment to evaluate
     * @param sourceAccount the source account
     * @param destinationAccount the destination account
     * @return evaluation result with score and explanation
     */
    FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    );
    
    /**
     * Get the weight of this rule in the overall score calculation
     * Higher weight = more impact on final fraud score
     */
    double getWeight();
}

