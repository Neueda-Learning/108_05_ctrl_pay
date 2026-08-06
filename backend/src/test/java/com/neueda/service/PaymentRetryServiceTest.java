package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentRetryServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    private RetryConfiguration retryConfig;
    private PaymentRetryService retryService;

    private PaymentRecord failedPayment;
    private PaymentRecord completedPayment;

    @BeforeEach
    void setUp() {
        retryConfig = new RetryConfiguration();
        retryConfig.setMaxAttempts(3);
        retryConfig.setInitialDelayMs(1000);

        retryService = new PaymentRetryService(paymentRepository, paymentService, retryConfig);

        failedPayment = new PaymentRecord(
            1L, "KEY123", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE,
            PaymentStatus.FAILED, "ERR01", "Failure reason", 1, 3,
            LocalDateTime.now(), null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        completedPayment = new PaymentRecord(
            2L, "KEY124", "111122223333", "444455556666", BigDecimal.valueOf(100), "USD",
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ONE,
            PaymentStatus.COMPLETED, null, null, 1, 3,
            null, null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("retryFailedPayment: Successfully resets status to VALIDATED")
    void retryFailedPayment_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(failedPayment));
        when(paymentRepository.update(any(PaymentRecord.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRecord retried = retryService.retryFailedPayment(1L);

        assertThat(retried.status()).isEqualTo(PaymentStatus.VALIDATED);
        assertThat(retried.errorCode()).isNull();
        assertThat(retried.errorMessage()).isNull();
        verify(paymentRepository).update(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("retryFailedPayment: Throws IllegalStateException when status is not FAILED")
    void retryFailedPayment_NotFailed() {
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(completedPayment));

        assertThatThrownBy(() -> retryService.retryFailedPayment(2L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot retry payment in COMPLETED status");
    }

    @Test
    @DisplayName("retryFailedPayment: Throws IllegalArgumentException when payment not found")
    void retryFailedPayment_NotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retryService.retryFailedPayment(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("calculateBackoffDelay: Computes exponential delay")
    void calculateBackoffDelay() {
        assertThat(retryService.calculateBackoffDelay(0)).isEqualTo(1000L);
        assertThat(retryService.calculateBackoffDelay(1)).isEqualTo(2000L);
        assertThat(retryService.calculateBackoffDelay(2)).isEqualTo(4000L);
    }

    @Test
    @DisplayName("canRetry: Validates max attempt limit")
    void canRetry() {
        assertThat(retryService.canRetry(0)).isTrue();
        assertThat(retryService.canRetry(2)).isTrue();
        assertThat(retryService.canRetry(3)).isFalse();
        assertThat(retryService.canRetry(4)).isFalse();
    }
}
