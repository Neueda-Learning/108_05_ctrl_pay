package com.neueda.fraud.rules;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Contextual Risk Aggregation Rule
 * Flags payments that combine several moderate-risk factors simultaneously
 * (large amount + cross-currency + low destination balance = elevated composite risk).
 */
@Component
public class ContextualRiskAggregationRule implements FraudRule {

    @Value("${fraud.rule.contextual-risk.large-amount:50000}")
    private double largeAmountThreshold;

    @Value("${fraud.rule.contextual-risk.low-dest-balance:500}")
    private double lowDestBalanceThreshold;

    @Value("${fraud.rule.contextual-risk.weight:0.06}")
    private double weight;

    @Override
    public String getRuleName() { return "CONTEXTUAL_RISK_AGGREGATION"; }

    @Override
    public String getDescription() { return "Flags payments that combine multiple moderate-risk contextual factors"; }

    @Override
    public double getWeight() { return weight; }

    @Override
    public FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        int riskFactors = 0;
        StringBuilder reasons = new StringBuilder();

        // Factor 1: Large amount
        if (payment.amount().compareTo(BigDecimal.valueOf(largeAmountThreshold)) >= 0) {
            riskFactors++;
            reasons.append("large amount; ");
        }

        // Factor 2: Cross-currency
        if (!sourceAccount.currency().equals(destinationAccount.currency())) {
            riskFactors++;
            reasons.append("cross-currency transfer; ");
        }

        // Factor 3: Low destination balance (potential money-mule account)
        if (destinationAccount.accountBalance().compareTo(BigDecimal.valueOf(lowDestBalanceThreshold)) < 0) {
            riskFactors++;
            reasons.append("destination has low pre-payment balance; ");
        }

        // Factor 4: Amount > destination pre-existing balance (sudden inflation)
        if (payment.amount().compareTo(destinationAccount.accountBalance()) > 0) {
            riskFactors++;
            reasons.append("payment exceeds destination's current balance; ");
        }

        if (riskFactors >= 3) {
            return FraudRuleResult.triggered(getRuleName(), 35 * riskFactors,
                "Multiple contextual risk factors: " + reasons.toString().trim());
        } else if (riskFactors == 2) {
            return FraudRuleResult.triggered(getRuleName(), 20,
                "Two contextual risk factors: " + reasons.toString().trim());
        }
        return FraudRuleResult.notTriggered(getRuleName(), "Contextual risk factors within acceptable range");
    }
}
