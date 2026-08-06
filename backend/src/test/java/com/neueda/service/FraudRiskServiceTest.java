package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.PaymentRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FraudRiskServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RestTemplate restTemplate;

    private FraudRiskService fraudRiskService;

    private PaymentRecord completedPayment;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;

    @BeforeEach
    void setUp() {
        fraudRiskService = new FraudRiskService(
            paymentRepository, accountRepository, restTemplate, "http://localhost:5000"
        );

        completedPayment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE,
            PaymentStatus.COMPLETED, null, null, 1, 3,
            null, null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        sourceAccount = AccountRecord.create(
            101L, "111122223333", "Source", BigDecimal.valueOf(900), "USD",
            LocalDate.now(), "IFSC01", "NY", "Bank A", "1234"
        );

        destAccount = AccountRecord.create(
            102L, "444455556666", "Dest", BigDecimal.valueOf(600), "USD",
            LocalDate.now(), "IFSC02", "NY", "Bank B", "5678"
        );
    }

    @Test
    @DisplayName("assessPaymentRisk: Null or non-COMPLETED payment returns null probability")
    void assessPaymentRisk_NonCompleted() {
        FraudRiskService.PaymentRisk riskNull = fraudRiskService.assessPaymentRisk(null);
        assertThat(riskNull.fraudProbability()).isNull();
        assertThat(riskNull.highRisk()).isFalse();

        PaymentRecord pendingPayment = completedPayment.withStatus(PaymentStatus.CREATED);
        FraudRiskService.PaymentRisk riskPending = fraudRiskService.assessPaymentRisk(pendingPayment);
        assertThat(riskPending.fraudProbability()).isNull();
        assertThat(riskPending.highRisk()).isFalse();
    }

    @Test
    @DisplayName("assessPaymentRisk: Calls external ML API and returns highRisk boolean")
    void assessPaymentRisk_Success() {
        when(accountRepository.findByAccountNumber("111122223333")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("444455556666")).thenReturn(Optional.of(destAccount));

        Map<String, Object> body = Map.of("fraud_probability", 85.5);
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(eq("http://localhost:5000/predict-json"), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenReturn(response);

        FraudRiskService.PaymentRisk risk = fraudRiskService.assessPaymentRisk(completedPayment);

        assertThat(risk.fraudProbability()).isEqualTo(85.5);
        assertThat(risk.highRisk()).isTrue();
    }

    @Test
    @DisplayName("refreshAccountRiskStatus: Marks account SUSPICIOUS if threshold exceeded")
    void refreshAccountRiskStatus_ElevatesStatus() {
        when(accountRepository.findByAccountNumber("111122223333")).thenReturn(Optional.of(sourceAccount));

        // Create 5 failed/suspicious payments
        List<PaymentRecord> recentPayments = List.of(
            createSuspiciousPayment("111122223333"),
            createSuspiciousPayment("111122223333"),
            createSuspiciousPayment("111122223333"),
            createSuspiciousPayment("111122223333"),
            createSuspiciousPayment("111122223333")
        );
        when(paymentRepository.findAll()).thenReturn(recentPayments);

        FraudRiskService.AccountRiskSummary summary = fraudRiskService.refreshAccountRiskStatus("111122223333");

        assertThat(summary.suspicious()).isTrue();
        assertThat(summary.highRiskTransactionsLast30Days()).isEqualTo(5);
        verify(accountRepository).update(any(AccountRecord.class));
    }

    private PaymentRecord createSuspiciousPayment(String accountNum) {
        return new PaymentRecord(
            null, "K", accountNum, "444455556666", BigDecimal.TEN, "USD",
            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, PaymentStatus.SUSPICIOUS,
            null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
