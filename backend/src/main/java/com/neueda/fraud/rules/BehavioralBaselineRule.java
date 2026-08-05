package com.neueda.fraud.rules;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Behavioral Baseline Rule — detects suspicion signals based on account status context.
 * Elevated risk when source or destination account is SUSPICIOUS or DORMANT.
 */
@Component
public class BehavioralBaselineRule implements FraudRule {

    @Value("${fraud.rule.behavioral-baseline.weight:0.08}")
    private double weight;

    @Override
    public String getRuleName() { return "BEHAVIORAL_BASELINE"; }

    @Override
    public String getDescription() { return "Detects baseline anomalies based on account status and history"; }

    @Override
    public double getWeight() { return weight; }

    @Override
    public FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        AccountStatus srcStatus = sourceAccount.accountStatus();
        AccountStatus dstStatus = destinationAccount.accountStatus();

        if (srcStatus == AccountStatus.SUSPICIOUS) {
            return FraudRuleResult.triggered(getRuleName(), 60,
                "Source account is flagged SUSPICIOUS — high fraud risk baseline");
        }
        if (dstStatus == AccountStatus.SUSPICIOUS) {
            return FraudRuleResult.triggered(getRuleName(), 50,
                "Destination account is flagged SUSPICIOUS — high fraud risk baseline");
        }
        if (srcStatus == AccountStatus.DORMANT) {
            return FraudRuleResult.triggered(getRuleName(), 30,
                "Payment from DORMANT source account — unusual activity");
        }
        if (dstStatus == AccountStatus.DORMANT) {
            return FraudRuleResult.triggered(getRuleName(), 20,
                "Payment to DORMANT destination account — unusual target");
        }
        return FraudRuleResult.notTriggered(getRuleName(), "Account statuses normal — no baseline anomaly");
    }
}
