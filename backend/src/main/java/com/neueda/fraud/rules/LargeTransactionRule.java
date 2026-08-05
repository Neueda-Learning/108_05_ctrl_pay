package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * Rule 1: Large Transaction Compared To Balance
 * Detects when payment amount exceeds configurable percentage of available balance
 */
@Component
public class LargeTransactionRule implements FraudRule {
    
    @Value("${fraud.rule.large-transaction.threshold-percent:80}")
    private double thresholdPercent;
    
    @Value("${fraud.rule.large-transaction.weight:0.15}")
    private double weight;

    @Override
    public String getRuleName() {
        return "LARGE_TRANSACTION";
    }

    @Override
    public String getDescription() {
        return "Transaction amount exceeds " + thresholdPercent + "% of available balance";
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
        BigDecimal balance = sourceAccount.accountBalance();
        BigDecimal amount = payment.amount();
        
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return FraudRuleResult.triggered(
                getRuleName(), 
                50,
                "Account has zero or negative balance"
            );
        }
        
        // Calculate percentage of balance
        double percentageOfBalance = amount.doubleValue() / balance.doubleValue() * 100;
        
        if (percentageOfBalance > thresholdPercent) {
            int score = (int) Math.min(100, (percentageOfBalance / thresholdPercent) * 25);
            return FraudRuleResult.triggered(
                getRuleName(),
                score,
                String.format("Transaction is %.1f%% of account balance (threshold: %.1f%%)", 
                    percentageOfBalance, thresholdPercent)
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Transaction within balance threshold");
    }
}

