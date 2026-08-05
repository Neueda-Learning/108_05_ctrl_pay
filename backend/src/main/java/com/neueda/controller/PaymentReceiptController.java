package com.neueda.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.PaymentRecord;
import com.neueda.exception.PaymentNotFoundException;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.exception.ReceiptAccessDeniedException;
import com.neueda.service.AccountService;
import com.neueda.service.PaymentService;
import com.neueda.service.ReceiptService;

/**
 * Customer-scoped receipt download controller.
 */
@RestController
@RequestMapping("/api/customers/{customerId}/payments/{paymentId}")
public class PaymentReceiptController {

    private final PaymentService paymentService;
    private final AccountService accountService;
    private final ReceiptService receiptService;

    public PaymentReceiptController(
        PaymentService paymentService,
        AccountService accountService,
        ReceiptService receiptService
    ) {
        this.paymentService = paymentService;
        this.accountService = accountService;
        this.receiptService = receiptService;
    }

    @GetMapping(value = "/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReceipt(
        @PathVariable Long customerId,
        @PathVariable Long paymentId
    ) {
        try {
            PaymentRecord payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

            boolean authorized = accountService.customerOwnsAnyAccount(
                customerId,
                payment.sourceAccount(),
                payment.destinationAccount()
            );

            if (!authorized) {
                throw new ReceiptAccessDeniedException(
                    "Customer " + customerId + " is not authorized to download receipt for payment " + paymentId
                );
            }

            byte[] pdfBytes = receiptService.generateReceiptPdf(payment, customerId);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payment-receipt-" + paymentId + ".pdf")
                .body(pdfBytes);
        } catch (PaymentNotFoundException | ReceiptAccessDeniedException | com.neueda.exception.PaymentValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentProcessingException("Error downloading payment receipt: " + ex.getMessage());
        }
    }
}
