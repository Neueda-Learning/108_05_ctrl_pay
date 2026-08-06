package com.neueda.scheduler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;
import com.neueda.service.PaymentService;
import com.neueda.service.PaymentSettlementService;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentSettlementService settlementService;
    @Mock
    private PaymentSchedulerProperties properties;

    private PaymentProcessorScheduler scheduler;

    private PaymentRecord validatedPayment;
    private PaymentRecord sentPayment;

    @BeforeEach
    void setUp() {
        when(properties.getIntervalMs()).thenReturn(5000);
        when(properties.getFailureRate()).thenReturn(0.05);

        scheduler = new PaymentProcessorScheduler(
            paymentRepository, paymentService, settlementService, properties
        );

        validatedPayment = new PaymentRecord(
            1L, "KEY1", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        sentPayment = new PaymentRecord(
            2L, "KEY2", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.SENT, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("processValidatedPayments: Processes all validated payments")
    void processValidatedPayments_Success() {
        when(paymentRepository.findAll(PaymentStatus.VALIDATED, 100, 0))
            .thenReturn(List.of(validatedPayment));
        when(paymentService.processValidatedPaymentToSent(1L)).thenReturn(sentPayment);

        scheduler.processValidatedPayments();

        verify(paymentService).processValidatedPaymentToSent(1L);
    }

    @Test
    @DisplayName("processSentPayments: Settles ready sent payments")
    void processSentPayments_Success() {
        when(paymentRepository.findAll(PaymentStatus.SENT, 100, 0))
            .thenReturn(List.of(sentPayment));

        PaymentRecord completedPayment = new PaymentRecord(
            2L, "KEY2", "ACC1", "ACC2", BigDecimal.valueOf(500), "USD",
            BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.ONE,
            PaymentStatus.COMPLETED, null, null, 1, 3, null, null, LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(completedPayment));

        scheduler.processSentPayments();

        verify(settlementService).settlePayment(2L);
    }
}
