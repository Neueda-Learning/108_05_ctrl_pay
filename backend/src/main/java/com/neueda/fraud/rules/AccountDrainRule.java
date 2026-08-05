package com.neueda.fraud.rules;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Rule 3: Account Drain Detection
 * Detects when remaining balance would fall below minimum threshold
 */
@Component
public class AccountDrainRule implements FraudRule {
    
    @Value("${fraud.rule.account-drain.minimum-balance:100}")
    private double minimumBalance;
    
    @Value("${fraud.rule.account-drain.weight:0.15}")
    private double weight;

    @Override
    public String getRuleName() {
        return "ACCOUNT_DRAIN";
    }

    @Override
    public String getDescription() {
        return "Payment would leave account balance below " + minimumBalance + " threshold";
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
        BigDecimal remainingBalance = balance.subtract(amount);
        
        if (remainingBalance.compareTo(BigDecimal.valueOf(minimumBalance)) < 0) {
            int score = Math.min(100, (int) (25 - (remainingBalance.doubleValue() / minimumBalance * 25)));
            return FraudRuleResult.triggered(
                getRuleName(),
                score,
                String.format("Account would be drained to %.2f (minimum: %.2f)", 
                    remainingBalance.doubleValue(), minimumBalance)
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Account would maintain adequate balance");
    }
}

