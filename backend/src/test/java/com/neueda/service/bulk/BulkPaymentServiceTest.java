package com.neueda.service.bulk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

import com.neueda.domain.AccountRecord;
import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.dto.BulkPaymentItemDTO;
import com.neueda.dto.CSVValidationResultDTO;
import com.neueda.dto.CreateBulkPaymentRequest;
import com.neueda.fraud.FraudDetectionService;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.repository.BulkPaymentItemRepository;
import com.neueda.repository.ValidationRuleRepository;
import com.neueda.service.AccountService;
import com.neueda.service.PaymentService;
import com.neueda.service.PaymentSettlementService;
import com.neueda.validation.RuleEngine;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BulkPaymentServiceTest {

    @Mock private BulkPaymentBatchRepository batchRepository;
    @Mock private BulkPaymentItemRepository itemRepository;
    @Mock private PaymentService paymentService;
    @Mock private PaymentSettlementService paymentSettlementService;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private RuleEngine ruleEngine;
    @Mock private com.neueda.repository.PaymentRepository paymentRepository;
    @Mock private ValidationRuleRepository validationRuleRepository;
    @Mock private AccountService accountService;

    private BulkPaymentService bulkPaymentService;

    private AccountRecord sourceAcc;
    private AccountRecord destAcc;

    @BeforeEach
    void setUp() {
        bulkPaymentService = new BulkPaymentService(
            batchRepository, itemRepository, paymentService, paymentSettlementService,
            fraudDetectionService, ruleEngine, paymentRepository, validationRuleRepository, accountService
        );

        sourceAcc = AccountRecord.create(
            1L, "111122223333", "Source", BigDecimal.valueOf(10000), "USD",
            LocalDate.now(), "IFSC1", "NY", "Bank A", "1234"
        );

        destAcc = AccountRecord.create(
            2L, "444455556666", "Dest", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC2", "NY", "Bank B", "5678"
        );
    }

    @Test
    @DisplayName("validateCSVUpload: Parses valid CSV header and lines successfully")
    void validateCSVUpload_Valid() throws Exception {
        String csvContent = "destinationAccount,amount\n444455556666,150.00\n";
        InputStream is = new ByteArrayInputStream(csvContent.getBytes());

        CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(is);

        assertThat(result.isValid()).isTrue();
        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.validRecords()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("validateCSVUpload: Returns validation error on invalid header")
    void validateCSVUpload_InvalidHeader() throws Exception {
        String csvContent = "invalid_header_1,invalid_header_2\n444455556666,150.00\n";
        InputStream is = new ByteArrayInputStream(csvContent.getBytes());

        CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(is);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage()).contains("Invalid CSV header format");
    }

    @Test
    @DisplayName("createBulkPayment: Successfully creates batch and items when PIN and accounts are valid")
    void createBulkPayment_Success() throws Exception {
        CreateBulkPaymentRequest req = new CreateBulkPaymentRequest(
            "111122223333", "1234",
            List.of(new BulkPaymentItemDTO("444455556666", BigDecimal.valueOf(100), "USD", "Payment 1")),
            "IDEM_BULK_1"
        );

        when(accountService.verifyAccountPinByAccountNumber("111122223333", "1234")).thenReturn(true);
        when(accountService.getAccountByAccountNumber("444455556666")).thenReturn(Optional.of(destAcc));
        when(accountService.getAccountByAccountNumber("111122223333")).thenReturn(Optional.of(sourceAcc));

        BulkPaymentBatchRecord savedBatch = BulkPaymentBatchRecord.create("BP1001", "IDEM_BULK_1", "111122223333", BigDecimal.valueOf(100), 1, "user1");
        when(batchRepository.create(any())).thenReturn(savedBatch);

        var resp = bulkPaymentService.createBulkPayment(req, "user1");

        assertThat(resp).isNotNull();
        verify(batchRepository).create(any());
        verify(itemRepository).createBatch(any());
    }
}
