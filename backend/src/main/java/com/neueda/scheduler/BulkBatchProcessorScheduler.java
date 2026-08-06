package com.neueda.scheduler;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.domain.BulkPaymentBatchStatus;
import com.neueda.exception.BulkPaymentBatchNotFoundException;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.service.bulk.BulkPaymentService;

/**
 * Bulk Batch Processor Scheduler - Handles lifecycle of bulk payment batches.
 * 
 * This scheduler processes bulk payment batches in the following phases:
 * 1. CREATED -> VALIDATING: Validate all items in batch
 * 2. VALIDATED -> PROCESSING: Process settlement for validated items
 * 
 * Each phase runs in a SEPARATE transaction to prevent cascading rollbacks.
 * 
 * Key Design:
 * - Batch creation completes quickly without blocking
 * - Validation and settlement happen asynchronously via scheduler
 * - Failure in settlement does NOT affect batch/item records
 * - Can be disabled via 'scheduler.bulk.enabled=false' configuration
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.bulk.enabled", havingValue = "true", matchIfMissing = true)
public class BulkBatchProcessorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkBatchProcessorScheduler.class);
    
    private final BulkPaymentBatchRepository batchRepository;
    private final BulkPaymentService bulkPaymentService;
    private final BulkBatchSchedulerProperties schedulerProperties;
    
    public BulkBatchProcessorScheduler(
        BulkPaymentBatchRepository batchRepository,
        BulkPaymentService bulkPaymentService,
        BulkBatchSchedulerProperties schedulerProperties
    ) {
        this.batchRepository = batchRepository;
        this.bulkPaymentService = bulkPaymentService;
        this.schedulerProperties = schedulerProperties;
    }
    
    /**
     * Process CREATED bulk batches through validation phase.
     * 
     * Each batch validation runs in its own transaction (via @Transactional 
     * on validateBatch method in BulkPaymentService).
     * 
     * Runs every N seconds (configured via scheduler.bulk.interval-ms).
     */
    @Scheduled(fixedRateString = "${scheduler.bulk.interval-ms:10000}")
    public void processCreatedBatches() {
        try {
            logger.debug("Starting validation of CREATED bulk payment batches");
            
            // Find batches in CREATED status (limit to prevent overwhelming scheduler)
            List<BulkPaymentBatchRecord> createdBatches = batchRepository.findByStatus(
                BulkPaymentBatchStatus.CREATED,
                schedulerProperties.getBatchSize(),
                0
            );
            
            if (createdBatches.isEmpty()) {
                logger.debug("No CREATED bulk batches found to validate");
                return;
            }
            
            logger.info("Found {} CREATED batches to validate", createdBatches.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (BulkPaymentBatchRecord batch : createdBatches) {
                try {
                    logger.info("Starting validation for batch: {}", batch.batchReference());
                    
                    // Each batch validation runs in a SEPARATE transaction
                    // This ensures that if validation fails, it doesn't affect batch record
                    bulkPaymentService.validateBatch(batch.id());
                    
                    successCount++;
                    logger.info("Successfully validated batch: {}", batch.batchReference());
                    
                } catch (BulkPaymentBatchNotFoundException e) {
                    logger.warn("Batch not found during validation: {}", batch.id());
                    failureCount++;
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Failed to validate batch {}: {}", batch.batchReference(), e.getMessage(), e);
                    // Continue processing other batches even if this one fails
                }
            }
            
            logger.info("Completed batch validation: {} successful, {} failed",
                successCount, failureCount);
            
        } catch (Exception e) {
            logger.error("Error in processCreatedBatches scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process VALIDATED bulk batches through settlement phase.
     * 
     * Each batch settlement runs in its own transaction (via @Transactional 
     * on processBatchSettlement method in BulkPaymentService).
     * 
     * This allows payment settlement to proceed independently and prevents
     * settlement failures from affecting batch validation results.
     * 
     * Runs every N seconds (configured via scheduler.bulk.interval-ms, 
     * with slight delay to allow validation to complete first).
     */
    @Scheduled(fixedRateString = "${scheduler.bulk.interval-ms:10000}", initialDelayString = "${scheduler.bulk.initial-delay-ms:5000}")
    public void processValidatedBatches() {
        try {
            logger.debug("Starting settlement of VALIDATED bulk payment batches");
            
            // Find batches in VALIDATED status
            List<BulkPaymentBatchRecord> validatedBatches = batchRepository.findByStatus(
                BulkPaymentBatchStatus.VALIDATED,
                schedulerProperties.getBatchSize(),
                0
            );
            
            if (validatedBatches.isEmpty()) {
                logger.debug("No VALIDATED bulk batches found to process");
                return;
            }
            
            logger.info("Found {} VALIDATED batches to process", validatedBatches.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (BulkPaymentBatchRecord batch : validatedBatches) {
                try {
                    logger.info("Starting settlement for batch: {}", batch.batchReference());
                    
                    // Each batch settlement runs in a SEPARATE transaction
                    // This ensures that settlement failures don't rollback earlier phases
                    bulkPaymentService.processBatchSettlement(batch.id());
                    
                    successCount++;
                    logger.info("Successfully processed batch: {}", batch.batchReference());
                    
                } catch (BulkPaymentBatchNotFoundException e) {
                    logger.warn("Batch not found during settlement: {}", batch.id());
                    failureCount++;
                } catch (Exception e) {
                    failureCount++;
                    logger.error("Failed to process batch {}: {}", batch.batchReference(), e.getMessage(), e);
                    // Continue processing other batches even if this one fails
                }
            }
            
            logger.info("Completed batch settlement: {} successful, {} failed",
                successCount, failureCount);
            
        } catch (Exception e) {
            logger.error("Error in processValidatedBatches scheduler: {}", e.getMessage(), e);
        }
    }
}

