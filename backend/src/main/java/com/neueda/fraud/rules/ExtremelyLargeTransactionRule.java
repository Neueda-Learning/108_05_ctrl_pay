package com.neueda.fraud.rules;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Rule 2: Extremely Large Transaction
 * Detects when payment amount exceeds configured absolute threshold
 */
@Component
public class ExtremelyLargeTransactionRule implements FraudRule {
    
    @Value("${fraud.rule.extremely-large.threshold:10000}")
    private double threshold;
    
    @Value("${fraud.rule.extremely-large.weight:0.12}")
    private double weight;

    @Override
    public String getRuleName() {
        return "EXTREMELY_LARGE_TRANSACTION";
    }

    @Override
    public String getDescription() {
        return "Transaction amount exceeds " + threshold + " (absolute threshold)";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        double amount = payment.amount().doubleValue();
        
        if (amount > threshold) {
            int score = (int) Math.min(100, (amount / threshold) * 20);
            return FraudRuleResult.triggered(
                getRuleName(),
                score,
                String.format("Transaction amount %.2f exceeds threshold %.2f", amount, threshold)
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Transaction within amount threshold");
    }
}

