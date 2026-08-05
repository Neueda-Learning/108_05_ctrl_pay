package com.neueda.fraud.rules;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.PaymentRepository;

/**
 * Rule 6: New Destination Account Detection
 * Detects when sending to a previously unknown recipient
 */
@Component
public class NewDestinationRule implements FraudRule {
    
    @Value("${fraud.rule.new-destination.new-account-days:30}")
    private int newAccountThresholdDays;
    
    @Value("${fraud.rule.new-destination.weight:0.10}")
    private double weight;
    
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    public NewDestinationRule(
        PaymentRepository paymentRepository,
        AccountRepository accountRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public String getRuleName() {
        return "NEW_DESTINATION";
    }

    @Override
    public String getDescription() {
        return "Detects transactions to new or unfamiliar recipient accounts";
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
        // Check if destination account is newly created
        LocalDateTime newAccountThreshold = LocalDateTime.now().minusDays(newAccountThresholdDays);
        if (destinationAccount.lastUpdated().isAfter(newAccountThreshold)) {
            return FraudRuleResult.triggered(
                getRuleName(),
                15,
                "Destination account was recently created"
            );
        }
        
        // Check if there's a prior transaction history to this destination
        List<PaymentRecord> all = paymentRepository.findAll();
        long priorTransactions = all.stream()
            .filter(p -> p.sourceAccount().equals(payment.sourceAccount()))
            .filter(p -> p.destinationAccount().equals(payment.destinationAccount()))
            .filter(p -> p.status() == PaymentStatus.COMPLETED || p.status() == PaymentStatus.VALIDATED)
            .count();
        
        if (priorTransactions == 0) {
            return FraudRuleResult.triggered(
                getRuleName(),
                12,
                "No prior transaction history to this recipient"
            );
        }
        
        return FraudRuleResult.notTriggered(getRuleName(), "Established recipient account");
    }
}

