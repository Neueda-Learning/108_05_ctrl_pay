package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.dto.CurrencyConversionRequest;
import com.neueda.dto.CurrencyConversionResponse;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private PaymentStatusHistoryRepository paymentStatusHistoryRepository;

    private PaymentSettlementService settlementService;

    private AccountRecord sourceAcc;
    private AccountRecord destAcc;
    private PaymentRecord sentPayment;

    @BeforeEach
    void setUp() {
        settlementService = new PaymentSettlementService(
            paymentRepository, accountRepository, currencyConversionService, paymentStatusHistoryRepository
        );

        sourceAcc = AccountRecord.create(
            101L, "111122223333", "Source Acc", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC01", "NY", "Bank A", "1234"
        );

        destAcc = AccountRecord.create(
            102L, "444455556666", "Dest Acc", BigDecimal.valueOf(500), "EUR",
            LocalDate.now(), "IFSC02", "London", "Bank B", "5678"
        );

        sentPayment = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(200), "USD",
            null, null, null, PaymentStatus.SENT, null, null,
            0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("settlePayment: Successful settlement debits source, credits dest with conversion, and updates status")
    void settlePayment_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(sentPayment));
        when(accountRepository.findByAccountNumber("111122223333")).thenReturn(Optional.of(sourceAcc));
        when(accountRepository.findByAccountNumber("444455556666")).thenReturn(Optional.of(destAcc));
        when(currencyConversionService.convert(any(CurrencyConversionRequest.class)))
            .thenReturn(new CurrencyConversionResponse("USD", "EUR", BigDecimal.valueOf(200), BigDecimal.valueOf(0.85), BigDecimal.valueOf(170)));

        settlementService.settlePayment(1L);

        verify(accountRepository, org.mockito.Mockito.times(2)).update(any(AccountRecord.class));
        verify(paymentRepository).update(any(PaymentRecord.class));
        verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistoryRecord.class));
    }

    @Test
    @DisplayName("settlePayment: Idempotent - already COMPLETED payment skips processing")
    void settlePayment_AlreadyCompleted() {
        PaymentRecord completed = new PaymentRecord(
            1L, "KEY1", "111122223333", "444455556666", BigDecimal.valueOf(200), "USD",
            BigDecimal.valueOf(200), BigDecimal.valueOf(170), BigDecimal.valueOf(0.85),
            PaymentStatus.COMPLETED, null, null, 1, 3, null, null,
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(completed));

        settlementService.settlePayment(1L);

        verify(accountRepository, never()).update(any());
    }

    @Test
    @DisplayName("settlePayment: Insufficient funds marks payment as FAILED")
    void settlePayment_InsufficientFunds() {
        AccountRecord lowBalanceSource = sourceAcc.withNewBalance(BigDecimal.valueOf(50));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(sentPayment));
        when(accountRepository.findByAccountNumber("111122223333")).thenReturn(Optional.of(lowBalanceSource));
        when(accountRepository.findByAccountNumber("444455556666")).thenReturn(Optional.of(destAcc));

        settlementService.settlePayment(1L);

        verify(paymentRepository).update(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("settlePayment: Source account missing marks payment as FAILED")
    void settlePayment_AccountNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(sentPayment));
        when(accountRepository.findByAccountNumber("111122223333")).thenReturn(Optional.empty());

        settlementService.settlePayment(1L);

        verify(paymentRepository).update(any(PaymentRecord.class));
    }
}
