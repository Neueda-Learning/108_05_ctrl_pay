package com.neueda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.exception.PaymentValidationException;

class ReceiptServiceTest {

    private final ReceiptService receiptService = new ReceiptService();

    @Test
    void shouldGenerateReceiptPdfForCompletedPayment() throws IOException {
        PaymentRecord payment = completedPayment();

        byte[] pdfBytes = receiptService.generateReceiptPdf(payment, 42L);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text).contains("Payment Receipt");
            assertThat(text).contains("Customer ID: 42");
            assertThat(text).contains("Payment ID: 1001");
            assertThat(text).contains("Status: SUCCESS");
            assertThat(text).contains("Source account: 123456789012");
        }
    }

    @Test
    void shouldRejectReceiptGenerationForNonCompletedPayment() {
        PaymentRecord payment = new PaymentRecord(
            1001L,
            "idem-1001",
            "123456789012",
            "210987654321",
            new BigDecimal("149.99"),
            "USD",
            new BigDecimal("149.99"),
            new BigDecimal("149.99"),
            BigDecimal.ONE,
            PaymentStatus.SENT,
            null,
            null,
            1,
            3,
            null,
            null,
            null,
            LocalDateTime.of(2026, 8, 5, 10, 0, 0),
            LocalDateTime.of(2026, 8, 5, 10, 5, 0)
        );

        assertThrows(PaymentValidationException.class, () -> receiptService.generateReceiptPdf(payment, 42L));
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
