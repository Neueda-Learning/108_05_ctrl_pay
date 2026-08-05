package com.neueda.fraud.rules;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.service.FraudRiskService;

/**
 * Rule 10: ML Fraud Prediction
 * Integrates machine learning fraud probability from external ML model
 */
@Component
public class MLFraudRule implements FraudRule {
    
    @Value("${fraud.rule.ml-fraud.weight:0.25}")
    private double weight;
    
    private final FraudRiskService fraudRiskService;

    public MLFraudRule(FraudRiskService fraudRiskService) {
        this.fraudRiskService = fraudRiskService;
    }

    @Override
    public String getRuleName() {
        return "ML_FRAUD_PREDICTION";
    }

    @Override
    public String getDescription() {
        return "Machine learning model fraud probability assessment";
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
        try {
            // Use assessPaymentRiskForCreation — works for any payment state (not just COMPLETED)
            FraudRiskService.PaymentRisk risk = fraudRiskService.assessPaymentRiskForCreation(payment);
            
            if (risk.fraudProbability() != null) {
                // Convert probability (0-100 from ML service) to score (0-100)
                int score = (int) Math.min(100, Math.max(0, risk.fraudProbability()));
                
                if (score > 50) {
                    return FraudRuleResult.triggered(
                        getRuleName(),
                        score,
                        String.format("ML model fraud probability: %.1f%%", risk.fraudProbability())
                    );
                } else {
                    return FraudRuleResult.notTriggered(
                        getRuleName(),
                        String.format("ML model fraud probability: %.1f%% (low risk)", risk.fraudProbability())
                    );
                }
            }
        } catch (Exception e) {
            // ML service unavailable — fallback gracefully, don't fail payment
            return FraudRuleResult.notTriggered(getRuleName(), "ML service unavailable (fallback to rules)");
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "No ML prediction available");
    }
}

