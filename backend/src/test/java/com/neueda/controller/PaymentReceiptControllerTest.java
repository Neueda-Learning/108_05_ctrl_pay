package com.neueda.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.AccountService;
import com.neueda.service.PaymentService;
import com.neueda.service.ReceiptService;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private AccountService accountService;

    @Mock
    private ReceiptService receiptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentReceiptController controller = new PaymentReceiptController(paymentService, accountService, receiptService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldDownloadReceiptForAuthorizedCustomer() throws Exception {
        PaymentRecord payment = completedPayment();
        byte[] pdfBytes = "%PDF-test".getBytes(StandardCharsets.UTF_8);

        when(paymentService.getPaymentById(1001L)).thenReturn(Optional.of(payment));
        when(accountService.customerOwnsAnyAccount(42L, "123456789012", "210987654321")).thenReturn(true);
        when(receiptService.generateReceiptPdf(payment, 42L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/customers/42/payments/1001/receipt"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", containsString("payment-receipt-1001.pdf")))
            .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void shouldReturnForbiddenWhenCustomerDoesNotOwnPaymentAccounts() throws Exception {
        PaymentRecord payment = completedPayment();

        when(paymentService.getPaymentById(1001L)).thenReturn(Optional.of(payment));
        when(accountService.customerOwnsAnyAccount(42L, "123456789012", "210987654321")).thenReturn(false);

        mockMvc.perform(get("/api/customers/42/payments/1001/receipt"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("RECEIPT_ACCESS_DENIED"))
            .andExpect(jsonPath("$.message", containsString("permission")));
    }

    private PaymentRecord completedPayment() {
        return new PaymentRecord(
            1001L,
            "idem-1001",
            "123456789012",
            "210987654321",
            new BigDecimal("149.99"),
            "USD",
            new BigDecimal("149.99"),
            new BigDecimal("149.99"),
            BigDecimal.ONE,
            PaymentStatus.COMPLETED,
            null,
            null,
            1,
            3,
            LocalDateTime.of(2026, 8, 5, 10, 1, 0),
            null,
            LocalDateTime.of(2026, 8, 5, 10, 5, 0),
            LocalDateTime.of(2026, 8, 5, 10, 0, 0),
            LocalDateTime.of(2026, 8, 5, 10, 5, 0)
        );
    }
}
