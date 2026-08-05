package com.neueda.fraud.rules;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * Rule 5: Sudden Transaction Pattern Change
 * Detects significant deviation from customer's historical behavior
 */
@Component
public class BehaviorChangeRule implements FraudRule {
    
    @Value("${fraud.rule.behavior-change.deviation-threshold:3.0}")
    private double deviationThreshold; // standard deviations
    
    @Value("${fraud.rule.behavior-change.lookback-days:30}")
    private int lookbackDays;
    
    @Value("${fraud.rule.behavior-change.weight:0.12}")
    private double weight;
    
    private final PaymentRepository paymentRepository;

    public BehaviorChangeRule(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String getRuleName() {
        return "BEHAVIOR_CHANGE";
    }

    @Override
    public String getDescription() {
        return "Detects significant deviation from historical transaction patterns";
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
        
        // Get historical transactions from this account (completed only)
        List<PaymentRecord> historicalPayments = all.stream()
            .filter(p -> p.sourceAccount().equals(payment.sourceAccount()))
            .filter(p -> p.status() == PaymentStatus.COMPLETED)
            .filter(p -> p.createdAt().isAfter(lookbackDate))
            .toList();
        
        if (historicalPayments.size() < 3) {
            return FraudRuleResult.notTriggered(getRuleName(), "Insufficient historical data");
        }
        
        // Calculate average and standard deviation
        double[] amounts = historicalPayments.stream()
            .mapToDouble(p -> p.amount().doubleValue())
            .toArray();
        
        double average = java.util.Arrays.stream(amounts).average().orElse(0);
        double variance = java.util.Arrays.stream(amounts)
            .map(x -> Math.pow(x - average, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);
        
        double currentAmount = payment.amount().doubleValue();
        double zScore = stdDev > 0 ? Math.abs((currentAmount - average) / stdDev) : 0;
        
        if (zScore > deviationThreshold) {
            int score = Math.min(100, (int) (20 * (zScore / deviationThreshold)));
            return FraudRuleResult.triggered(
                getRuleName(),
                score,
                String.format(
                    "Amount %.2f is %.2f std devs from historical average %.2f", 
                    currentAmount, zScore, average
                )
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Transaction matches historical pattern");
    }
}

