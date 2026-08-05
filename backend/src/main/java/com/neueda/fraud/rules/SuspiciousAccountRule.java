package com.neueda.fraud.rules;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;

/**
 * Rule 8: Suspicious Account Status Detection
 * Automatically increases fraud score if source account is marked SUSPICIOUS
 */
@Component
public class SuspiciousAccountRule implements FraudRule {
    
    @Value("${fraud.rule.suspicious-account.weight:0.20}")
    private double weight;

    @Override
    public String getRuleName() {
        return "SUSPICIOUS_ACCOUNT";
    }

    @Override
    public String getDescription() {
        return "Source account is marked as SUSPICIOUS";
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
        if (sourceAccount.accountStatus() == AccountStatus.SUSPICIOUS) {
            return FraudRuleResult.triggered(
                getRuleName(),
                100,
                "Source account is in SUSPICIOUS status"
            );
        }
        
        if (destinationAccount.accountStatus() == AccountStatus.SUSPICIOUS) {
            return FraudRuleResult.triggered(
                getRuleName(),
                50,
                "Destination account is in SUSPICIOUS status"
            );
        }
        
        if (sourceAccount.accountStatus() == AccountStatus.DORMANT) {
            return FraudRuleResult.triggered(
                getRuleName(),
                30,
                "Source account is DORMANT"
            );
        }
        
        if (sourceAccount.accountStatus() == AccountStatus.PASSIVE) {
            return FraudRuleResult.triggered(
                getRuleName(),
                15,
                "Source account is PASSIVE"
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Source account has good status");
    }
}

