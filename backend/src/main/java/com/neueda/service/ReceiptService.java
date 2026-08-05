package com.neueda.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.exception.PaymentValidationException;

/**
 * Generates payment receipt PDFs on demand without persisting files to disk.
 */
@Service
public class ReceiptService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateReceiptPdf(PaymentRecord payment, Long customerId) {
        if (payment.status() != PaymentStatus.COMPLETED) {
            throw new PaymentValidationException(
                "Receipt is only available for successful payments",
                "RECEIPT_NOT_AVAILABLE"
            );
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float startX = 50;
                float startY = page.getMediaBox().getHeight() - 60;

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(startX, startY);
                contentStream.showText("Payment Receipt");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setLeading(18f);
                contentStream.newLineAtOffset(startX, startY - 35);

                for (String line : buildReceiptLines(payment, customerId)) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }

                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new PaymentProcessingException("Error generating payment receipt: " + ex.getMessage());
        }
    }

    private List<String> buildReceiptLines(PaymentRecord payment, Long customerId) {
        List<String> lines = new ArrayList<>();
        lines.add("Receipt generated at: " + formatTimestamp(LocalDateTime.now()));
        lines.add("Customer ID: " + customerId);
        lines.add("Payment ID: " + payment.id());
        lines.add("Status: SUCCESS");
        lines.add("Amount: " + payment.amount().toPlainString() + " " + payment.currency());
        lines.add("Source account: " + payment.sourceAccount());
        lines.add("Destination account: " + payment.destinationAccount());
        lines.add("Created at: " + formatTimestamp(payment.createdAt()));
        lines.add("Completed at: " + formatTimestamp(resolveCompletionTime(payment)));

        if (payment.sourceAmount() != null) {
            lines.add("Debited amount: " + payment.sourceAmount().toPlainString());
        }
        if (payment.destinationAmount() != null) {
            lines.add("Credited amount: " + payment.destinationAmount().toPlainString());
        }
        if (payment.exchangeRate() != null) {
            lines.add("Exchange rate: " + payment.exchangeRate().toPlainString());
        }
        if (payment.idempotencyKey() != null && !payment.idempotencyKey().isBlank()) {
            lines.add("Idempotency key: " + payment.idempotencyKey());
        }

        return lines;
    }

    private LocalDateTime resolveCompletionTime(PaymentRecord payment) {
        return payment.settledAt() != null ? payment.settledAt() : payment.updatedAt();
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        return timestamp != null ? timestamp.format(TIMESTAMP_FORMATTER) : "N/A";
    }
}
