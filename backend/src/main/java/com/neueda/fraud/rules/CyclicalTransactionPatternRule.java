package com.neueda.fraud.rules;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * Cyclical Transaction Pattern Rule
 * Detects when the destination account recently sent money back to the source
 * (indicates potential round-trip / layering fraud).
 */
@Component
public class CyclicalTransactionPatternRule implements FraudRule {

    @Value("${fraud.rule.cyclical-pattern.lookback-hours:24}")
    private int lookbackHours;

    @Value("${fraud.rule.cyclical-pattern.weight:0.08}")
    private double weight;

    private final PaymentRepository paymentRepository;

    public CyclicalTransactionPatternRule(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String getRuleName() { return "CYCLICAL_TRANSACTION_PATTERN"; }

    @Override
    public String getDescription() { return "Detects circular fund movements (destination recently sent to source)"; }

    @Override
    public double getWeight() { return weight; }

    @Override
    public FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        try {
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(lookbackHours);
            List<PaymentRecord> all = paymentRepository.findAll();

            boolean cyclicalFound = all.stream()
                .filter(p -> !p.status().equals(PaymentStatus.FAILED))
                .filter(p -> p.createdAt().isAfter(cutoff))
                // Check if destination recently sent to source (A→B already, now B→A)
                .anyMatch(p -> p.sourceAccount().equals(payment.destinationAccount())
                            && p.destinationAccount().equals(payment.sourceAccount()));

            if (cyclicalFound) {
                return FraudRuleResult.triggered(getRuleName(), 50,
                    "Cyclical transaction detected: destination recently transferred back to source within " + lookbackHours + "h");
            }
        } catch (Exception e) {
            // Don't fail fraud detection due to query error
        }
        return FraudRuleResult.notTriggered(getRuleName(), "No cyclical pattern detected");
    }
}
