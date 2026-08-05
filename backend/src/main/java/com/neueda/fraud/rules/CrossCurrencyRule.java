package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Rule 9: Cross Currency High Risk Detection
 * Detects high-risk international transfers
 */
@Component
public class CrossCurrencyRule implements FraudRule {
    
    @Value("${fraud.rule.cross-currency.high-risk-threshold:5000}")
    private double highRiskAmountThreshold;
    
    @Value("${fraud.rule.cross-currency.weight:0.08}")
    private double weight;

    @Override
    public String getRuleName() {
        return "CROSS_CURRENCY_RISK";
    }

    @Override
    public String getDescription() {
        return "Detects high-risk international currency transfers";
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
        // Check if currencies are different (international transfer)
        if (!payment.currency().equals(sourceAccount.currency())) {
            double amount = payment.amount().doubleValue();
            
            // Check amount threshold for cross-currency transfers
            if (amount > highRiskAmountThreshold) {
                int score = Math.min(100, (int) (15 * (amount / highRiskAmountThreshold)));
                return FraudRuleResult.triggered(
                    getRuleName(),
                    score,
                    String.format(
                        "High-value cross-currency transfer: %.2f from %s to %s", 
                        amount, sourceAccount.currency(), payment.currency()
                    )
                );
            }
            
            // Moderate risk for any cross-currency transfer
            return FraudRuleResult.triggered(
                getRuleName(),
                8,
                String.format("Cross-currency transfer from %s to %s", 
                    sourceAccount.currency(), payment.currency())
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Domestic same-currency transfer");
    }
}

