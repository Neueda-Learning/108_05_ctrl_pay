package com.neueda.service.bulk;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.domain.BulkPaymentBatchStatus;
import com.neueda.domain.BulkPaymentItemRecord;
import com.neueda.domain.BulkPaymentItemStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.ValidationResultRecord;
import com.neueda.domain.ValidationRuleRecord;
import com.neueda.dto.BulkPaymentItemDTO;
import com.neueda.dto.BulkPaymentProgressDTO;
import com.neueda.dto.BulkPaymentResponseDTO;
import com.neueda.dto.CSVValidationResultDTO;
import com.neueda.dto.CreateBulkPaymentRequest;
import com.neueda.exception.AccountValidationException;
import com.neueda.exception.BulkPaymentBatchNotFoundException;
import com.neueda.exception.BulkPaymentCSVValidationException;
import com.neueda.fraud.FraudDetectionService;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.repository.BulkPaymentItemRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.ValidationRuleRepository;
import com.neueda.service.AccountService;
import com.neueda.service.PaymentService;
import com.neueda.service.PaymentSettlementService;
import com.neueda.validation.RuleEngine;

@ExtendWith(MockitoExtension.class)
class BulkPaymentServiceTest {

    @Mock
    private BulkPaymentBatchRepository batchRepository;
    @Mock
    private BulkPaymentItemRepository itemRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentSettlementService paymentSettlementService;
    @Mock
    private FraudDetectionService fraudDetectionService;
    @Mock
    private RuleEngine ruleEngine;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ValidationRuleRepository validationRuleRepository;
    @Mock
    private AccountService accountService;

    private BulkPaymentService bulkPaymentService;
    private BulkPaymentBatchRecord sampleBatch;
    private AccountRecord sourceAccount;
    private AccountRecord destAccount;

    @BeforeEach
    void setUp() {
        bulkPaymentService = new BulkPaymentService(
            batchRepository, itemRepository, paymentService, paymentSettlementService,
            fraudDetectionService, ruleEngine, paymentRepository, validationRuleRepository, accountService
        );

        sampleBatch = BulkPaymentBatchRecord.create("BP123", "KEY1", "111122223333", new BigDecimal("100.00"), 1, "USER1");
        sourceAccount = new AccountRecord(1L, 10L, "111122223333", "Source Acc", new BigDecimal("10000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
        destAccount = new AccountRecord(2L, 11L, "444455556666", "Dest Acc", new BigDecimal("5000"), AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");
    }

    @Test
    @DisplayName("validateCSVUpload: Success with valid header and row")
    void validateCSVUpload_Success() throws Exception {
        String csvContent = "destinationAccount,amount,currency,idempotencyKey\n" +
            "444455556666,100.00,USD,KEY1\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(inputStream);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(1, result.totalRecords());
    }

    @Test
    @DisplayName("validateCSVUpload: Invalid header format")
    void validateCSVUpload_InvalidHeader() throws Exception {
        String csvContent = "Invalid,Header\n111122223333,444455556666,100.00,USD,KEY1\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(inputStream);

        assertNotNull(result);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("validateCSVUpload: Invalid row data with negative amount")
    void validateCSVUpload_InvalidRowData() throws Exception {
        String csvContent = "destinationAccount,amount,currency,idempotencyKey\n" +
            "444455556666,-50.00,USD,KEY1\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(inputStream);

        assertNotNull(result);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("createBulkPayment: Success")
    void createBulkPayment_Success() throws Exception {
        BulkPaymentItemDTO itemDTO = new BulkPaymentItemDTO("444455556666", new BigDecimal("100.00"), "USD", "Test");
        CreateBulkPaymentRequest request = new CreateBulkPaymentRequest("111122223333", "1234", List.of(itemDTO), "KEY1");

        when(batchRepository.findByIdempotencyKey("KEY1")).thenReturn(Optional.empty());
        when(accountService.getAccountByAccountNumber("111122223333")).thenReturn(Optional.of(sourceAccount));
        when(accountService.getAccountByAccountNumber("444455556666")).thenReturn(Optional.of(destAccount));
        when(batchRepository.create(any())).thenReturn(sampleBatch);

        BulkPaymentResponseDTO response = bulkPaymentService.createBulkPayment(request, "USER1");

        assertNotNull(response);
        assertEquals("BP123", response.batchReference());
        verify(itemRepository).createBatch(any());
    }

    @Test
    @DisplayName("createBulkPayment: Idempotent request returns existing batch")
    void createBulkPayment_Idempotent() throws Exception {
        BulkPaymentItemDTO itemDTO = new BulkPaymentItemDTO("444455556666", new BigDecimal("100.00"), "USD", "Test");
        CreateBulkPaymentRequest request = new CreateBulkPaymentRequest("111122223333", "1234", List.of(itemDTO), "KEY1");

        when(batchRepository.findByIdempotencyKey("KEY1")).thenReturn(Optional.of(sampleBatch));

        BulkPaymentResponseDTO response = bulkPaymentService.createBulkPayment(request, "USER1");

        assertNotNull(response);
        assertEquals("BP123", response.batchReference());
    }

    @Test
    @DisplayName("createBulkPayment: Invalid PIN throws exception")
    void createBulkPayment_InvalidPin() throws Exception {
        CreateBulkPaymentRequest request = new CreateBulkPaymentRequest("111122223333", "9999", List.of(), "KEY1");
        doThrow(new AccountValidationException("Invalid PIN", "INVALID_PIN"))
            .when(accountService).verifyAccountPinByAccountNumber("111122223333", "9999");

        assertThrows(BulkPaymentCSVValidationException.class, () -> bulkPaymentService.createBulkPayment(request, "USER1"));
    }

    @Test
    @DisplayName("validateBatch: Batch not found throws exception")
    void validateBatch_NotFound() {
        when(batchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BulkPaymentBatchNotFoundException.class, () -> bulkPaymentService.validateBatch(99L));
    }

    @Test
    @DisplayName("validateBatch: Success validating items")
    void validateBatch_Success() throws Exception {
        BulkPaymentItemRecord item = BulkPaymentItemRecord.create(1L, 1, "444455556666", new BigDecimal("100.00"), "USD", "Desc");
        ValidationRuleRecord activeRule = new ValidationRuleRecord(1L, "AMOUNT_RANGE", "Amount check", com.neueda.domain.RuleType.AMOUNT_RANGE, null, true, com.neueda.domain.Severity.HARD, 1, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());

        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));
        when(itemRepository.findByBatchId(1L)).thenReturn(List.of(item));
        when(validationRuleRepository.findActiveRules()).thenReturn(List.of(activeRule));
        when(ruleEngine.validatePayment(any(), any())).thenReturn(List.of(ValidationResultRecord.success(1L, 1L, "AMOUNT_RANGE", null, 1)));

        assertDoesNotThrow(() -> bulkPaymentService.validateBatch(1L));

        verify(batchRepository).update(any());
    }

    @Test
    @DisplayName("processBatchSettlement: Batch not found throws exception")
    void processBatchSettlement_NotFound() {
        when(batchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BulkPaymentBatchNotFoundException.class, () -> bulkPaymentService.processBatchSettlement(99L));
    }

    @Test
    @DisplayName("processBatchSettlement: Success settling validated items")
    void processBatchSettlement_Success() throws Exception {
        BulkPaymentItemRecord item = BulkPaymentItemRecord.create(1L, 1, "444455556666", new BigDecimal("100.00"), "USD", "Desc").withValidationCompleted();
        PaymentRecord mockCreatedPayment = new PaymentRecord(10L, null, "111122223333", "444455556666", new BigDecimal("100.00"), "USD", null, null, null, PaymentStatus.VALIDATED, null, null, 0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());

        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));
        when(itemRepository.findByBatchIdAndStatus(1L, BulkPaymentItemStatus.VALIDATED)).thenReturn(List.of(item));
        when(accountService.getAccountByAccountNumber("111122223333")).thenReturn(Optional.of(sourceAccount));
        when(accountService.getAccountByAccountNumber("444455556666")).thenReturn(Optional.of(destAccount));
        when(paymentService.createPayment(any())).thenReturn(mockCreatedPayment);

        assertDoesNotThrow(() -> bulkPaymentService.processBatchSettlement(1L));
    }

    @Test
    @DisplayName("getBatchDetails: Success")
    void getBatchDetails_Success() throws Exception {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));

        BulkPaymentResponseDTO response = bulkPaymentService.getBatchDetails(1L);

        assertNotNull(response);
        assertEquals("BP123", response.batchReference());
    }

    @Test
    @DisplayName("getBatchDetailsByReference: Success")
    void getBatchDetailsByReference_Success() throws Exception {
        when(batchRepository.findByReference("BP123")).thenReturn(Optional.of(sampleBatch));

        BulkPaymentResponseDTO response = bulkPaymentService.getBatchDetailsByReference("BP123");

        assertNotNull(response);
        assertEquals("BP123", response.batchReference());
    }

    @Test
    @DisplayName("getProgress: Calculates percentages correctly")
    void getProgress_Success() throws Exception {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));
        when(itemRepository.countByBatchIdAndStatus(1L, BulkPaymentItemStatus.VALIDATED)).thenReturn(0);
        when(itemRepository.countByBatchIdAndStatus(1L, BulkPaymentItemStatus.SUCCESS)).thenReturn(1);
        when(itemRepository.countByBatchIdAndStatus(1L, BulkPaymentItemStatus.FAILED)).thenReturn(0);

        BulkPaymentProgressDTO progress = bulkPaymentService.getProgress(1L);

        assertNotNull(progress);
        assertEquals(100, progress.progressPercentage());
    }
}
