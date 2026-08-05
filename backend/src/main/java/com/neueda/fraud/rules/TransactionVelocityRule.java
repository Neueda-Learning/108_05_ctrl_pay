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
 * Rule 4: Transaction Velocity Detection
 * Detects unusual frequency of transactions in short time windows
 */
@Component
public class TransactionVelocityRule implements FraudRule {
    
    @Value("${fraud.rule.velocity.limit-5min:5}")
    private int maxTransactionsIn5Min;
    
    @Value("${fraud.rule.velocity.limit-24h:20}")
    private int maxTransactionsIn24h;
    
    @Value("${fraud.rule.velocity.weight:0.12}")
    private double weight;
    
    private final PaymentRepository paymentRepository;

    public TransactionVelocityRule(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String getRuleName() {
        return "TRANSACTION_VELOCITY";
    }

    @Override
    public String getDescription() {
        return "Detects unusually high frequency of transactions";
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        // Find recent transactions from this source account
        List<PaymentRecord> all = paymentRepository.findAll();
        
        long last5MinCount = all.stream()
            .filter(p -> p.sourceAccount().equals(payment.sourceAccount()))
            .filter(p -> p.createdAt().isAfter(fiveMinutesAgo))
            .filter(p -> !p.status().equals(PaymentStatus.FAILED))
            .count();
        
        long last24hCount = all.stream()
            .filter(p -> p.sourceAccount().equals(payment.sourceAccount()))
            .filter(p -> p.createdAt().isAfter(oneDayAgo))
            .filter(p -> !p.status().equals(PaymentStatus.FAILED))
            .count();
        
        // Check 5-minute window
        if (last5MinCount >= maxTransactionsIn5Min) {
            return FraudRuleResult.triggered(
                getRuleName(),
                40,
                String.format("Too many transactions in last 5 minutes: %d (max: %d)", 
                    last5MinCount, maxTransactionsIn5Min)
            );
        }
        
        // Check 24-hour window
        if (last24hCount >= maxTransactionsIn24h) {
            return FraudRuleResult.triggered(
                getRuleName(),
                25,
                String.format("High transaction frequency in last 24h: %d (max: %d)", 
                    last24hCount, maxTransactionsIn24h)
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Transaction velocity within normal range");
    }
}

