package com.neueda.fraud.rules;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * Rule 7: Multiple Failed Payments Detection
 * Detects when customer has had multiple failed/rejected payments recently
 */
@Component
public class MultipleFailureRule implements FraudRule {
    
    @Value("${fraud.rule.multiple-failure.threshold:3}")
    private int failureThreshold;
    
    @Value("${fraud.rule.multiple-failure.lookback-days:7}")
    private int lookbackDays;
    
    @Value("${fraud.rule.multiple-failure.weight:0.10}")
    private double weight;
    
    private final PaymentRepository paymentRepository;

    public MultipleFailureRule(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String getRuleName() {
        return "MULTIPLE_FAILURES";
    }

    @Override
    public String getDescription() {
        return "Detects accounts with multiple recent payment failures";
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
        LocalDateTime lookbackDate = LocalDateTime.now().minusDays(lookbackDays);
        List<PaymentRecord> all = paymentRepository.findAll();
        
        // Count failed payments in lookback window
        long failedCount = all.stream()
            .filter(p -> p.sourceAccount().equals(payment.sourceAccount()))
            .filter(p -> p.status() == PaymentStatus.FAILED)
            .filter(p -> p.createdAt().isAfter(lookbackDate))
            .count();
        
        if (failedCount >= failureThreshold) {
            int score = Math.min(100, (int) (20 * (failedCount / failureThreshold)));
            return FraudRuleResult.triggered(
                getRuleName(),
                score,
                String.format("Account has %d failed payments in last %d days (threshold: %d)", 
                    failedCount, lookbackDays, failureThreshold)
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Account has acceptable failure rate");
    }
}

