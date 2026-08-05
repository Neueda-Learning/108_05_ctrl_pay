package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.PaymentRepository;

/**
 * Read-side fraud risk analytics.
 *
 * Important: this service never changes payment workflow/state.
 * It only computes risk indicators and can elevate account status to SUSPICIOUS.
 */
@Service
public class FraudRiskService {

    private static final double HIGH_RISK_THRESHOLD = 80.0;
    private static final int SUSPICIOUS_COUNT_THRESHOLD = 5;
    private static final int RISK_WINDOW_DAYS = 30;

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;
    private final String fraudApiBaseUrl;

    public FraudRiskService(
        PaymentRepository paymentRepository,
        AccountRepository accountRepository,
        RestTemplate restTemplate,
        @Value("${fraud.api.base-url:http://localhost:5000}") String fraudApiBaseUrl
    ) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.restTemplate = restTemplate;
        this.fraudApiBaseUrl = fraudApiBaseUrl;
    }

    public record PaymentRisk(Double fraudProbability, boolean highRisk) {}

    public record AccountRiskSummary(int highRiskTransactionsLast30Days, boolean suspicious) {}

    public PaymentRisk assessPaymentRisk(PaymentRecord payment) {
        if (payment == null || payment.status() != PaymentStatus.COMPLETED) {
            return new PaymentRisk(null, false);
        }

        try {
            Double fraudProbability = fetchFraudProbability(payment);
            boolean highRisk = fraudProbability != null && fraudProbability >= HIGH_RISK_THRESHOLD;
            return new PaymentRisk(fraudProbability, highRisk);
        } catch (Exception ignored) {
            // Best-effort risk indication only; never fail payment/read APIs.
            return new PaymentRisk(null, false);
        }
    }

    public AccountRiskSummary refreshAccountRiskStatus(String accountNumber) {
        AccountRecord account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        LocalDateTime since = LocalDateTime.now().minusDays(RISK_WINDOW_DAYS);
        List<PaymentRecord> recentPayments = paymentRepository.findCompletedByAccountSince(accountNumber, since);

        int highRiskCount = 0;
        for (PaymentRecord payment : recentPayments) {
            if (assessPaymentRisk(payment).highRisk()) {
                highRiskCount++;
            }
        }

        boolean suspicious = highRiskCount >= SUSPICIOUS_COUNT_THRESHOLD;
        if (suspicious && account.accountStatus() != AccountStatus.SUSPICIOUS) {
            accountRepository.update(account.withStatus(AccountStatus.SUSPICIOUS));
        }

        return new AccountRiskSummary(highRiskCount, suspicious);
    }

    private Double fetchFraudProbability(PaymentRecord payment) {
        AccountRecord sourceAccount = accountRepository.findByAccountNumber(payment.sourceAccount())
            .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + payment.sourceAccount()));
        AccountRecord destinationAccount = accountRepository.findByAccountNumber(payment.destinationAccount())
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + payment.destinationAccount()));

        BigDecimal sourceDebitAmount = safeAmount(payment.sourceAmount() != null ? payment.sourceAmount() : payment.amount());
        BigDecimal destinationCreditAmount = safeAmount(payment.destinationAmount() != null ? payment.destinationAmount() : payment.amount());

        BigDecimal newSourceBalance = safeAmount(sourceAccount.accountBalance());
        BigDecimal oldSourceBalance = newSourceBalance.add(sourceDebitAmount);

        BigDecimal newDestinationBalance = safeAmount(destinationAccount.accountBalance());
        BigDecimal oldDestinationBalance = newDestinationBalance.subtract(destinationCreditAmount);
        if (oldDestinationBalance.compareTo(BigDecimal.ZERO) < 0) {
            oldDestinationBalance = BigDecimal.ZERO;
        }

        double amountValue = sourceDebitAmount.doubleValue();
        double oldbalanceOrg = oldSourceBalance.doubleValue();
        double newbalanceOrig = newSourceBalance.doubleValue();
        double oldbalanceDest = oldDestinationBalance.doubleValue();
        double newbalanceDest = newDestinationBalance.doubleValue();

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amountValue);
        payload.put("oldbalanceOrg", oldbalanceOrg);
        payload.put("newbalanceOrig", newbalanceOrig);
        payload.put("oldbalanceDest", oldbalanceDest);
        payload.put("newbalanceDest", newbalanceDest);
        payload.put("transaction_type", "TRANSFER");

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            fraudApiBaseUrl + "/predict-json",
            HttpMethod.POST,
            new HttpEntity<>(payload),
            new ParameterizedTypeReference<>() {}
        );

        if (response.getBody() == null) {
            return null;
        }

        Object value = response.getBody().get("fraud_probability");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }

        return null;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

