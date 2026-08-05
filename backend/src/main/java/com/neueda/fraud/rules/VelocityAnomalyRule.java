package com.neueda.fraud.rules;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Velocity Anomaly Rule — detects payment amount unusually large relative to account balance.
 * Complements AccountDrainRule by focusing on proportion rather than absolute minimum balance.
 */
@Component
public class VelocityAnomalyRule implements FraudRule {

    @Value("${fraud.rule.velocity-anomaly.high-ratio:0.5}")
    private double highRatioThreshold;  // 50% of balance

    @Value("${fraud.rule.velocity-anomaly.extreme-ratio:0.9}")
    private double extremeRatioThreshold;  // 90% of balance

    @Value("${fraud.rule.velocity-anomaly.weight:0.06}")
    private double weight;

    @Override
    public String getRuleName() { return "VELOCITY_ANOMALY"; }

    @Override
    public String getDescription() { return "Payment amount anomaly relative to account balance"; }

    @Override
    public double getWeight() { return weight; }

    @Override
    public FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        BigDecimal balance = sourceAccount.accountBalance();
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return FraudRuleResult.notTriggered(getRuleName(), "Balance is zero or unavailable");
        }

        double ratio = payment.amount().doubleValue() / balance.doubleValue();

        if (ratio >= extremeRatioThreshold) {
            return FraudRuleResult.triggered(getRuleName(), 40,
                String.format("Payment is %.1f%% of account balance — extreme drain risk", ratio * 100));
        } else if (ratio >= highRatioThreshold) {
            return FraudRuleResult.triggered(getRuleName(), 20,
                String.format("Payment is %.1f%% of account balance — high proportion", ratio * 100));
        }
        return FraudRuleResult.notTriggered(getRuleName(),
            String.format("Payment is %.1f%% of account balance — normal range", ratio * 100));
    }
}
