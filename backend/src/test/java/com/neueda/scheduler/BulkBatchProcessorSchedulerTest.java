package com.neueda.scheduler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.domain.BulkPaymentBatchStatus;
import com.neueda.exception.BulkPaymentBatchNotFoundException;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.service.bulk.BulkPaymentService;

@ExtendWith(MockitoExtension.class)
class BulkBatchProcessorSchedulerTest {

    @Mock
    private BulkPaymentBatchRepository batchRepository;

    @Mock
    private BulkPaymentService bulkPaymentService;

    @Mock
    private BulkBatchSchedulerProperties schedulerProperties;

    private BulkBatchProcessorScheduler scheduler;
    private BulkPaymentBatchRecord createdBatch;
    private BulkPaymentBatchRecord validatedBatch;
    private BulkPaymentBatchRecord processingBatch;

    @BeforeEach
    void setUp() {
        scheduler = new BulkBatchProcessorScheduler(batchRepository, bulkPaymentService, schedulerProperties);
        createdBatch = BulkPaymentBatchRecord.create("BATCH-001", "KEY-001", "111122223333", BigDecimal.valueOf(100), 10, "USER1");
        validatedBatch = createdBatch.withValidationCompleted();
        processingBatch = validatedBatch.withProcessingStarted();
    }

    @Test
    @DisplayName("processCreatedBatches: No CREATED batches does nothing")
    void processCreatedBatches_Empty() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.CREATED, 10, 0)).thenReturn(Collections.emptyList());

        scheduler.processCreatedBatches();

        verify(bulkPaymentService, never()).validateBatch(any());
    }

    @Test
    @DisplayName("processCreatedBatches: Validates CREATED batches successfully")
    void processCreatedBatches_Success() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.CREATED, 10, 0)).thenReturn(List.of(createdBatch));

        scheduler.processCreatedBatches();

        verify(bulkPaymentService).validateBatch(createdBatch.id());
    }

    @Test
    @DisplayName("processCreatedBatches: Handles BulkPaymentBatchNotFoundException")
    void processCreatedBatches_NotFoundException() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.CREATED, 10, 0)).thenReturn(List.of(createdBatch));
        doThrow(new BulkPaymentBatchNotFoundException("Not found")).when(bulkPaymentService).validateBatch(createdBatch.id());

        assertDoesNotThrow(() -> scheduler.processCreatedBatches());
    }

    @Test
    @DisplayName("processCreatedBatches: Handles general Exception during batch validation")
    void processCreatedBatches_GeneralException() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.CREATED, 10, 0)).thenReturn(List.of(createdBatch));
        doThrow(new RuntimeException("Database error")).when(bulkPaymentService).validateBatch(createdBatch.id());

        assertDoesNotThrow(() -> scheduler.processCreatedBatches());
    }

    @Test
    @DisplayName("processCreatedBatches: Handles exception thrown by repository query")
    void processCreatedBatches_RepositoryException() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(any(), eq(10), eq(0))).thenThrow(new RuntimeException("Fatal DB error"));

        assertDoesNotThrow(() -> scheduler.processCreatedBatches());
    }

    @Test
    @DisplayName("processValidatedBatches: No VALIDATED batches does nothing")
    void processValidatedBatches_Empty() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.VALIDATED, 10, 0)).thenReturn(Collections.emptyList());

        scheduler.processValidatedBatches();

        verify(bulkPaymentService, never()).processBatchSettlement(any());
    }

    @Test
    @DisplayName("processValidatedBatches: Processes settlement successfully")
    void processValidatedBatches_Success() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.VALIDATED, 10, 0)).thenReturn(List.of(validatedBatch));

        scheduler.processValidatedBatches();

        verify(bulkPaymentService).processBatchSettlement(validatedBatch.id());
    }

    @Test
    @DisplayName("processValidatedBatches: Handles BulkPaymentBatchNotFoundException")
    void processValidatedBatches_NotFoundException() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.VALIDATED, 10, 0)).thenReturn(List.of(validatedBatch));
        doThrow(new BulkPaymentBatchNotFoundException("Not found")).when(bulkPaymentService).processBatchSettlement(validatedBatch.id());

        assertDoesNotThrow(() -> scheduler.processValidatedBatches());
    }

    @Test
    @DisplayName("processValidatedBatches: Handles general exception during settlement")
    void processValidatedBatches_GeneralException() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.VALIDATED, 10, 0)).thenReturn(List.of(validatedBatch));
        doThrow(new RuntimeException("Settlement failure")).when(bulkPaymentService).processBatchSettlement(validatedBatch.id());

        assertDoesNotThrow(() -> scheduler.processValidatedBatches());
    }

    @Test
    @DisplayName("checkAndUpdateBatchCompletion: No PROCESSING batches does nothing")
    void checkAndUpdateBatchCompletion_Empty() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.PROCESSING, 10, 0)).thenReturn(Collections.emptyList());

        scheduler.checkAndUpdateBatchCompletion();

        verify(bulkPaymentService, never()).checkAndUpdateBatchCompletion(any());
    }

    @Test
    @DisplayName("checkAndUpdateBatchCompletion: Updates status of completed batches")
    void checkAndUpdateBatchCompletion_TransitionToCompleted() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.PROCESSING, 10, 0)).thenReturn(List.of(processingBatch));
        BulkPaymentBatchRecord completedBatch = processingBatch.withProcessingCompleted(BulkPaymentBatchStatus.COMPLETED);
        when(batchRepository.findById(processingBatch.id())).thenReturn(Optional.of(completedBatch));

        scheduler.checkAndUpdateBatchCompletion();

        verify(bulkPaymentService).checkAndUpdateBatchCompletion(processingBatch.id());
    }

    @Test
    @DisplayName("checkAndUpdateBatchCompletion: Batch remains in PROCESSING status")
    void checkAndUpdateBatchCompletion_RemainsProcessing() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.PROCESSING, 10, 0)).thenReturn(List.of(processingBatch));
        when(batchRepository.findById(processingBatch.id())).thenReturn(Optional.of(processingBatch));

        scheduler.checkAndUpdateBatchCompletion();

        verify(bulkPaymentService).checkAndUpdateBatchCompletion(processingBatch.id());
    }

    @Test
    @DisplayName("checkAndUpdateBatchCompletion: Handles exception during completion check")
    void checkAndUpdateBatchCompletion_Exception() {
        when(schedulerProperties.getBatchSize()).thenReturn(10);
        when(batchRepository.findByStatus(BulkPaymentBatchStatus.PROCESSING, 10, 0)).thenReturn(List.of(processingBatch));
        doThrow(new RuntimeException("Check failed")).when(bulkPaymentService).checkAndUpdateBatchCompletion(processingBatch.id());

        assertDoesNotThrow(() -> scheduler.checkAndUpdateBatchCompletion());
    }
}
